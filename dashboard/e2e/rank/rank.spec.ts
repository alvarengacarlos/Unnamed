import type { Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import { uniqueName } from "../support/unique"
import { chooseOption, clickRowAction, confirmDeletion, dialog, expectSuccessNotification, rowWith } from "../support/ui"

// Opened after the data of the test is created, so the page loads it
async function openRanks(page: Page): Promise<void> {
    await page.goto("/ranks")
    await page.getByRole("tab", { name: "Graus" }).click()
}

test.describe("Graus", () => {
    test("cria um grau", async ({ page, data }) => {
        const rankCategory = await data.rankCategory()
        const name = uniqueName("Faixa")
        await openRanks(page)

        await page.getByTestId("new-rank-button").click()
        const form = dialog(page, "Grau")
        await form.getByLabel("Nome").fill(name)
        await form.getByLabel("Ordem").fill("1")
        await chooseOption(page, form.getByLabel("Categoria"), rankCategory.name)
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Grau salvo")
        const row = rowWith(page.getByTestId("rank-body-table"), name)
        await expect(row).toContainText(rankCategory.name)
    })

    test("não cria um grau sem os campos obrigatórios", async ({ page }) => {
        await openRanks(page)

        await page.getByTestId("new-rank-button").click()
        const form = dialog(page, "Grau")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form.getByText("Nome é obrigatório")).toBeVisible()
        await expect(form.getByText("Ordem é obrigatório")).toBeVisible()
        await expect(form.getByText("Categoria é obrigatório")).toBeVisible()
    })

    test("não cria um grau com a ordem de outro da mesma categoria", async ({ page, data }) => {
        const rank = await data.rank({ order: 1 })
        await openRanks(page)

        await page.getByTestId("new-rank-button").click()
        const form = dialog(page, "Grau")
        await form.getByLabel("Nome").fill(uniqueName("Faixa"))
        await form.getByLabel("Ordem").fill("1")
        await chooseOption(page, form.getByLabel("Categoria"), rank.category.name)
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form).toContainText("Já existe um grau com esta ordem nesta categoria.")
    })

    test("mostra um grau", async ({ page, data }) => {
        const rank = await data.rank()
        await openRanks(page)

        await clickRowAction(rowWith(page.getByTestId("rank-body-table"), rank.name), "Ver")

        const view = page.getByTestId("rank-view-modal")
        await expect(view).toContainText(rank.id)
        await expect(view).toContainText(rank.name)
        await expect(view).toContainText(rank.category.name)
    })

    test("edita o nome de um grau, mas não a ordem nem a categoria", async ({ page, data }) => {
        const rank = await data.rank()
        const newName = uniqueName("Faixa")
        await openRanks(page)

        await clickRowAction(rowWith(page.getByTestId("rank-body-table"), rank.name), "Editar")
        const form = dialog(page, "Grau")
        await expect(form.getByLabel("Ordem")).toBeDisabled()
        await expect(form.getByLabel("Categoria")).toBeDisabled()
        await form.getByLabel("Nome").fill(newName)
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Grau salvo")
        const table = page.getByTestId("rank-body-table")
        await expect(rowWith(table, newName)).toContainText(rank.category.name)
        await expect(rowWith(table, rank.name)).toHaveCount(0)
    })

    test("deleta um grau", async ({ page, data }) => {
        const rank = await data.rank()
        await openRanks(page)

        await clickRowAction(rowWith(page.getByTestId("rank-body-table"), rank.name), "Deletar")
        await expect(dialog(page, "Deletar grau?")).toContainText(rank.name)
        await confirmDeletion(page, "Deletar grau?")

        await expectSuccessNotification(page, "Grau deletado")
        await expect(rowWith(page.getByTestId("rank-body-table"), rank.name)).toHaveCount(0)
    })

    test("cancela a deleção de um grau", async ({ page, data }) => {
        const rank = await data.rank()
        await openRanks(page)

        await clickRowAction(rowWith(page.getByTestId("rank-body-table"), rank.name), "Deletar")
        await dialog(page, "Deletar grau?").getByRole("button", { name: "Cancelar" }).click()

        await expect(dialog(page, "Deletar grau?")).toHaveCount(0)
        await expect(rowWith(page.getByTestId("rank-body-table"), rank.name)).toBeVisible()
    })

    test("não deleta um grau de algum membro", async ({ page, data }) => {
        const member = await data.member()
        await openRanks(page)

        await clickRowAction(rowWith(page.getByTestId("rank-body-table"), member.rank.name), "Deletar")
        await confirmDeletion(page, "Deletar grau?")

        await expect(dialog(page, "Deletar grau?"))
            .toContainText("Não é possível deletar: existem outros registros ligados a este.")
    })

    test("filtra os graus pela categoria", async ({ page, data }) => {
        const rank = await data.rank()
        const rankOfAnotherCategory = await data.rank()
        await openRanks(page)

        // The filter, outside the form of the rank
        await chooseOption(page, page.getByRole("tabpanel").getByLabel("Categoria"), rank.category.name)

        const table = page.getByTestId("rank-body-table")
        await expect(rowWith(table, rank.name)).toBeVisible()
        await expect(rowWith(table, rankOfAnotherCategory.name)).toHaveCount(0)
    })

    test("mostra os graus da página seguinte", async ({ page, data }) => {
        // More ranks than a page (50). The newest come first, so the first one created is on the second page.
        const category = await data.rankCategory()
        const firstRank = await data.rank({ order: 1, category })
        await Promise.all(Array.from({ length: 50 }, (_, i) => data.rank({ order: i + 2, category })))
        await openRanks(page)

        const table = page.getByTestId("rank-body-table")
        await expect(table.getByRole("row")).toHaveCount(50)
        await expect(rowWith(table, firstRank.name)).toHaveCount(0)

        await page.getByRole("tabpanel").getByRole("button", { name: "2", exact: true }).click()

        await expect(rowWith(table, firstRank.name)).toBeVisible()
    })
})
