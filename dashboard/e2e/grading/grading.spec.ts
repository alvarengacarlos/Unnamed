import type { Locator, Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import type { Rank } from "../../src/rank/rank.type"
import { uniqueName } from "../support/unique"
import { chooseDateTime, chooseOption, clickRowAction, confirmDeletion, dateInMonths, dialog, expectSuccessNotification, rowWith } from "../support/ui"
import { convertDateToView, convertPriceToView } from "../../src/common/common.converter"

// Opened after the data of the test is created, so the page loads it
async function openGradings(page: Page): Promise<void> {
    await page.goto("/gradings")
}

function rankOption(rank: Rank): string {
    return `${rank.name} - ${rank.category.name}`
}

// The fields of a grading, but the prices
async function fillGrading(page: Page, form: Locator, name: string, start: string, end: string): Promise<void> {
    await form.getByLabel("Nome").fill(name)
    await chooseDateTime(page, form.getByLabel("Data e hora"), dateInMonths(2, 15), "09:00")
    await form.getByLabel("Local").fill("Sede")
    await form.getByLabel("Início das inscrições").fill(convertDateToView(start))
    await form.getByLabel("Fim das inscrições").fill(convertDateToView(end))
}

test.describe("Graduações", () => {
    test("cria uma graduação com um preço por grau", async ({ page, data }) => {
        const rank = await data.rank()
        const anotherRank = await data.rank()
        const name = uniqueName("Graduação")
        const start = dateInMonths(1, 1)
        const end = dateInMonths(1, 20)
        await openGradings(page)

        await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Graduação")
        await fillGrading(page, form, name, start, end)
        await chooseOption(page, form.getByLabel(/^Grau/).nth(0), rankOption(rank))
        await form.getByLabel(/^Preço/).nth(0).fill("80")
        await form.getByRole("button", { name: "Adicionar preço" }).click()
        await chooseOption(page, form.getByLabel(/^Grau/).nth(1), rankOption(anotherRank))
        await form.getByLabel(/^Preço/).nth(1).fill("100")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Graduação salva")
        const row = rowWith(page.getByRole("tabpanel"), name)
        await expect(row).toContainText("Sede")
        await expect(row).toContainText(`${convertDateToView(start)} a ${convertDateToView(end)}`)

        await clickRowAction(row, "Ver")
        const view = page.getByRole("dialog")
        await expect(view).toContainText(`${rankOption(rank)}: ${convertPriceToView(80)}`)
        await expect(view).toContainText(`${rankOption(anotherRank)}: ${convertPriceToView(100)}`)
    })

    test("não cria uma graduação sem os campos obrigatórios", async ({ page }) => {
        await openGradings(page)

        await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Graduação")
        await form.getByRole("button", { name: "Remover preço" }).click()
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form.getByText("Nome é obrigatório")).toBeVisible()
        await expect(form.getByText("Data e hora são obrigatórios")).toBeVisible()
        await expect(form.getByText("Local é obrigatório")).toBeVisible()
        await expect(form.getByText("Início das inscrições é obrigatório")).toBeVisible()
        await expect(form.getByText("Fim das inscrições é obrigatório")).toBeVisible()
        await expect(form.getByText("Informe ao menos 1 preço")).toBeVisible()
    })

    test("não cria uma graduação cujas inscrições terminam antes de começar", async ({ page, data, fakeCoreApi }) => {
        const rank = await data.rank()
        await openGradings(page)

        await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Graduação")
        await fillGrading(page, form, uniqueName("Graduação"), dateInMonths(1, 20), dateInMonths(1, 19))
        await chooseOption(page, form.getByLabel(/^Grau/), rankOption(rank))
        await form.getByLabel(/^Preço/).fill("80")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form.getByText("O fim das inscrições não pode ser antes do início")).toBeVisible()
        expect(fakeCoreApi.requests.filter(r => r.method === "POST" && r.path === "/gradings")).toHaveLength(0)
    })

    test("não cria uma graduação com o mesmo grau em dois preços", async ({ page, data }) => {
        const rank = await data.rank()
        await openGradings(page)

        await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Graduação")
        await fillGrading(page, form, uniqueName("Graduação"), dateInMonths(1, 1), dateInMonths(1, 20))
        await chooseOption(page, form.getByLabel(/^Grau/).nth(0), rankOption(rank))
        await form.getByLabel(/^Preço/).nth(0).fill("80")
        await form.getByRole("button", { name: "Adicionar preço" }).click()
        await chooseOption(page, form.getByLabel(/^Grau/).nth(1), rankOption(rank))
        await form.getByLabel(/^Preço/).nth(1).fill("100")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form.getByText("Há grau repetido nos preços")).toBeVisible()
    })

    test("edita uma graduação", async ({ page, data }) => {
        const grading = await data.grading()
        const newName = uniqueName("Graduação")
        await openGradings(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), grading.name), "Editar")
        const form = dialog(page, "Graduação")
        await expect(form.getByLabel("Nome")).toHaveValue(grading.name)
        await form.getByLabel("Nome").fill(newName)
        await form.getByLabel("Local").fill("Ginásio novo")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Graduação salva")
        await expect(rowWith(page.getByRole("tabpanel"), newName)).toContainText("Ginásio novo")
        await expect(rowWith(page.getByRole("tabpanel"), grading.name)).toHaveCount(0)
    })

    test("deleta uma graduação", async ({ page, data }) => {
        const grading = await data.grading()
        await openGradings(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), grading.name), "Deletar")
        await expect(dialog(page, "Deletar graduação?")).toContainText(grading.name)
        await confirmDeletion(page, "Deletar graduação?")

        await expectSuccessNotification(page, "Graduação deletada")
        await expect(rowWith(page.getByRole("tabpanel"), grading.name)).toHaveCount(0)
    })

    test("não deleta uma graduação com candidatos", async ({ page, data }) => {
        const candidate = await data.gradingCandidate()
        await openGradings(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), candidate.grading.name), "Deletar")
        await confirmDeletion(page, "Deletar graduação?")

        await expect(dialog(page, "Deletar graduação?"))
            .toContainText("Não é possível deletar: existem outros registros ligados a este.")
    })
})
