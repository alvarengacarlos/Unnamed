import type { Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import { uniqueName } from "../support/unique"
import { clickRowAction, confirmDeletion, dialog, expectSuccessNotification, rowWith } from "../support/ui"
import { convertPriceToView } from "../../src/common/common.converter"

// Opened after the data of the test is created, so the page loads it
async function openTuitions(page: Page): Promise<void> {
    await page.goto("/tuitions")
}

test.describe("Mensalidades", () => {
    test("cria uma mensalidade", async ({ page }) => {
        const name = uniqueName("Mensalidade")
        await openTuitions(page)

        await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Mensalidade")
        await form.getByLabel("Nome").fill(name)
        await form.getByLabel("Preço").fill("150,5")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Mensalidade salva")
        await expect(rowWith(page.getByRole("tabpanel"), name)).toContainText(convertPriceToView(150.5))
    })

    test("não cria uma mensalidade sem os campos obrigatórios", async ({ page }) => {
        await openTuitions(page)

        await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Mensalidade")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form.getByText("Nome é obrigatório")).toBeVisible()
        await expect(form.getByText("Preço é obrigatório")).toBeVisible()
    })

    test("mostra uma mensalidade", async ({ page, data }) => {
        const tuition = await data.tuition()
        await openTuitions(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), tuition.name), "Ver")

        const view = page.getByRole("dialog")
        await expect(view).toContainText(tuition.id)
        await expect(view).toContainText(convertPriceToView(tuition.price))
    })

    test("edita uma mensalidade", async ({ page, data }) => {
        const tuition = await data.tuition()
        const newName = uniqueName("Mensalidade")
        await openTuitions(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), tuition.name), "Editar")
        const form = dialog(page, "Mensalidade")
        await expect(form.getByLabel("Nome")).toHaveValue(tuition.name)
        await form.getByLabel("Nome").fill(newName)
        await form.getByLabel("Preço").fill("200")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Mensalidade salva")
        await expect(rowWith(page.getByRole("tabpanel"), newName)).toContainText(convertPriceToView(200))
        await expect(rowWith(page.getByRole("tabpanel"), tuition.name)).toHaveCount(0)
    })

    test("deleta uma mensalidade", async ({ page, data }) => {
        const tuition = await data.tuition()
        await openTuitions(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), tuition.name), "Deletar")
        await expect(dialog(page, "Deletar mensalidade?")).toContainText(tuition.name)
        await confirmDeletion(page, "Deletar mensalidade?")

        await expectSuccessNotification(page, "Mensalidade deletada")
        await expect(rowWith(page.getByRole("tabpanel"), tuition.name)).toHaveCount(0)
    })

    test("não deleta uma mensalidade de alguma matrícula", async ({ page, data }) => {
        const studentEnrollment = await data.studentEnrollment()
        await openTuitions(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), studentEnrollment.tuition.name), "Deletar")
        await confirmDeletion(page, "Deletar mensalidade?")

        await expect(dialog(page, "Deletar mensalidade?"))
            .toContainText("Não é possível deletar: existem outros registros ligados a este.")
    })
})
