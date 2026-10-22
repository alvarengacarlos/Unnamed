import { test, expect } from "../fixtures"
import { uniqueName } from "../support/unique"
import { clickRowAction, confirmDeletion, dialog, expectSuccessNotification, rowWith } from "../support/ui"
import type { Page } from "@playwright/test"

// Opened after the data of the test is created, so the page loads it
async function openRankCategories(page: Page): Promise<void> {
    await page.goto("/ranks")
    await page.getByRole("tab", { name: "Categorias" }).click()
}

test.describe("Categorias de grau", () => {
    test("cria uma categoria", async ({ page }) => {
        await openRankCategories(page)
        const name = uniqueName("Categoria")

        await page.getByTestId("new-rank-category-button").click()
        const form = dialog(page, "Categoria")
        await form.getByLabel("Nome").fill(name)
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Categoria salva")
        await expect(rowWith(page.getByTestId("rank-category-body-table"), name)).toBeVisible()
    })

    test("não cria uma categoria sem nome", async ({ page }) => {
        await openRankCategories(page)
        await page.getByTestId("new-rank-category-button").click()
        const form = dialog(page, "Categoria")
        await form.getByLabel("Nome").fill("   ")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form.getByText("Nome é obrigatório")).toBeVisible()
    })

    test("mostra uma categoria", async ({ page, data }) => {
        const rankCategory = await data.rankCategory()
        await openRankCategories(page)

        await clickRowAction(rowWith(page.getByTestId("rank-category-body-table"), rankCategory.name), "Ver")

        const view = page.getByTestId("rank-category-view-modal")
        await expect(view).toContainText(rankCategory.id)
        await expect(view).toContainText(rankCategory.name)
    })

    test("edita uma categoria", async ({ page, data }) => {
        const rankCategory = await data.rankCategory()
        const newName = uniqueName("Categoria")
        await openRankCategories(page)

        await clickRowAction(rowWith(page.getByTestId("rank-category-body-table"), rankCategory.name), "Editar")
        const form = dialog(page, "Categoria")
        await expect(form.getByLabel("Nome")).toHaveValue(rankCategory.name)
        await form.getByLabel("Nome").fill(newName)
        await form.getByRole("button", { name: "Salvar" }).click()

        const table = page.getByTestId("rank-category-body-table")
        await expect(rowWith(table, newName)).toBeVisible()
        await expect(rowWith(table, rankCategory.name)).toHaveCount(0)
    })

    test("deleta uma categoria", async ({ page, data }) => {
        const rankCategory = await data.rankCategory()
        await openRankCategories(page)

        await clickRowAction(rowWith(page.getByTestId("rank-category-body-table"), rankCategory.name), "Deletar")
        await expect(dialog(page, "Deletar categoria?")).toContainText(rankCategory.name)
        await confirmDeletion(page, "Deletar categoria?")

        await expectSuccessNotification(page, "Categoria deletada")
        await expect(rowWith(page.getByTestId("rank-category-body-table"), rankCategory.name)).toHaveCount(0)
    })

    test("não deleta uma categoria com graus", async ({ page, data }) => {
        const rank = await data.rank()
        await openRankCategories(page)

        await clickRowAction(rowWith(page.getByTestId("rank-category-body-table"), rank.category.name), "Deletar")
        await confirmDeletion(page, "Deletar categoria?")

        await expect(dialog(page, "Deletar categoria?"))
            .toContainText("Não é possível deletar: existem outros registros ligados a este.")
        await expect(rowWith(page.getByTestId("rank-category-body-table"), rank.category.name)).toBeVisible()
    })
})
