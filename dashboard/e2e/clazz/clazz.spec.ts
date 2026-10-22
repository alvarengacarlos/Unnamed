import type { Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import { uniqueName } from "../support/unique"
import { chooseOption, clickRowAction, confirmDeletion, dialog, expectSuccessNotification, rowWith } from "../support/ui"
import { MartialArt } from "../../src/clazz/clazz.type"

// Opened after the data of the test is created, so the page loads it
async function openClazzes(page: Page): Promise<void> {
    await page.goto("/clazzes")
}

test.describe("Turmas", () => {
    test("cria uma turma", async ({ page, data }) => {
        const teacher = await data.teacher()
        const name = uniqueName("Turma")
        await openClazzes(page)

        await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Turma")
        await form.getByLabel("Nome").fill(name)
        await chooseOption(page, form.getByLabel("Arte marcial"), "Muay Thai")
        await form.getByLabel("Hora inicial").fill("07:30")
        await form.getByLabel("Hora final").fill("08:45")
        // The list of the MultiSelect stays open while the days are chosen
        await form.getByLabel("Dias da semana").click()
        await page.getByRole("option", { name: "Segunda-feira", exact: true }).click()
        await page.getByRole("option", { name: "Quarta-feira", exact: true }).click()
        await chooseOption(page, form.getByLabel("Professor"), teacher.member.name)
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Turma salva")
        const row = rowWith(page.getByRole("tabpanel"), name)
        await expect(row).toContainText("Muay Thai")
        await expect(row).toContainText("07:30 - 08:45")
        await expect(row).toContainText(teacher.member.name)

        await clickRowAction(row, "Ver")
        await expect(page.getByRole("dialog")).toContainText("Segunda-feira, Quarta-feira")
    })

    test("não cria uma turma sem os campos obrigatórios", async ({ page }) => {
        await openClazzes(page)

        await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Turma")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form.getByText("Nome é obrigatório")).toBeVisible()
        await expect(form.getByText("Arte marcial é obrigatório")).toBeVisible()
        await expect(form.getByText("Hora inicial é obrigatório")).toBeVisible()
        await expect(form.getByText("Hora final é obrigatório")).toBeVisible()
        await expect(form.getByText("Dias da semana é obrigatório")).toBeVisible()
        await expect(form.getByText("Professor é obrigatório")).toBeVisible()
    })

    test("mostra uma turma", async ({ page, data }) => {
        const clazz = await data.clazz()
        await openClazzes(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), clazz.name), "Ver")

        const view = page.getByRole("dialog")
        await expect(view).toContainText(clazz.id)
        await expect(view).toContainText("18:00 - 19:00")
        await expect(view).toContainText("Segunda-feira")
    })

    test("edita uma turma", async ({ page, data }) => {
        const clazz = await data.clazz()
        const newName = uniqueName("Turma")
        await openClazzes(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), clazz.name), "Editar")
        const form = dialog(page, "Turma")
        await expect(form.getByLabel("Nome")).toHaveValue(clazz.name)
        await form.getByLabel("Nome").fill(newName)
        await form.getByLabel("Hora final").fill("20:00")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Turma salva")
        await expect(rowWith(page.getByRole("tabpanel"), newName)).toContainText("18:00 - 20:00")
        await expect(rowWith(page.getByRole("tabpanel"), clazz.name)).toHaveCount(0)
    })

    test("deleta uma turma", async ({ page, data }) => {
        const clazz = await data.clazz()
        await openClazzes(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), clazz.name), "Deletar")
        await expect(dialog(page, "Deletar turma?")).toContainText(clazz.name)
        await confirmDeletion(page, "Deletar turma?")

        await expectSuccessNotification(page, "Turma deletada")
        await expect(rowWith(page.getByRole("tabpanel"), clazz.name)).toHaveCount(0)
    })

    test("não deleta uma turma com matrículas", async ({ page, data }) => {
        const studentEnrollment = await data.studentEnrollment()
        await openClazzes(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), studentEnrollment.clazz.name), "Deletar")
        await confirmDeletion(page, "Deletar turma?")

        await expect(dialog(page, "Deletar turma?"))
            .toContainText("Não é possível deletar: existem outros registros ligados a este.")
    })

    test("filtra as turmas pela arte marcial", async ({ page, data }) => {
        const judo = await data.clazz({ martialArt: MartialArt.JUDO })
        const karate = await data.clazz({ martialArt: MartialArt.KARATE })
        await openClazzes(page)

        await chooseOption(page, page.getByRole("tabpanel").getByLabel("Arte marcial"), "Karate")

        const panel = page.getByRole("tabpanel")
        await expect(rowWith(panel, karate.name)).toBeVisible()
        await expect(rowWith(panel, judo.name)).toHaveCount(0)
    })
})
