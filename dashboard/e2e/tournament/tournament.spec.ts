import type { Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import { uniqueName } from "../support/unique"
import { chooseDateTime, chooseOption, clickRowAction, confirmDeletion, dateInMonths, dialog, expectSuccessNotification, rowWith } from "../support/ui"
import { convertDateToView, convertPriceToView } from "../../src/common/common.converter"
import { TournamentFormat } from "../../src/tournament/tournament.type"

// Opened after the data of the test is created, so the page loads it
async function openTournaments(page: Page): Promise<void> {
    await page.goto("/tournaments")
}

test.describe("Torneios", () => {
    test("cria um torneio com um grau como critério de divisão", async ({ page, data }) => {
        const rank = await data.rank()
        const name = uniqueName("Torneio")
        const day = dateInMonths(1, 10)
        await openTournaments(page)

        await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Torneio")
        await form.getByLabel("Nome").fill(name)
        await chooseOption(page, form.getByLabel("Formato"), "Eliminatória simples")
        await form.getByLabel("Preço de participação").fill("50")
        await chooseDateTime(page, form.getByLabel("Início"), day, "09:00")
        await chooseDateTime(page, form.getByLabel("Fim"), day, "18:00")
        await form.getByLabel("Local").fill("Ginásio Municipal")
        await chooseOption(page, form.getByLabel("Grau"), `${rank.name} - ${rank.category.name}`)
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Torneio salvo")
        const row = rowWith(page.getByRole("tabpanel"), name)
        await expect(row).toContainText("Eliminatória simples")
        await expect(row).toContainText(`${convertDateToView(day)}, 09:00`)
        await expect(row).toContainText("Ginásio Municipal")
        await expect(row).toContainText(convertPriceToView(50))

        await clickRowAction(row, "Ver")
        await expect(page.getByRole("dialog")).toContainText(`Grau: ${rank.name} - ${rank.category.name}`)
    })

    test("não cria um torneio sem os campos obrigatórios", async ({ page }) => {
        await openTournaments(page)

        await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Torneio")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form.getByText("Nome é obrigatório")).toBeVisible()
        await expect(form.getByText("Formato é obrigatório")).toBeVisible()
        await expect(form.getByText("Preço de participação é obrigatório")).toBeVisible()
        await expect(form.getByText("Início é obrigatório")).toBeVisible()
        await expect(form.getByText("Fim é obrigatório")).toBeVisible()
        await expect(form.getByText("Local é obrigatório")).toBeVisible()
    })

    test("não cria um torneio que termina antes de começar", async ({ page, fakeCoreApi }) => {
        await openTournaments(page)

        await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Torneio")
        await form.getByLabel("Nome").fill(uniqueName("Torneio"))
        await chooseOption(page, form.getByLabel("Formato"), "Eliminatória simples")
        await form.getByLabel("Preço de participação").fill("50")
        await chooseDateTime(page, form.getByLabel("Início"), dateInMonths(1, 10), "09:00")
        await chooseDateTime(page, form.getByLabel("Fim"), dateInMonths(1, 9), "18:00")
        await form.getByLabel("Local").fill("Ginásio")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form.getByText("O fim não pode ser antes do início")).toBeVisible()
        expect(fakeCoreApi.requests.filter(r => r.method === "POST" && r.path === "/tournaments")).toHaveLength(0)
    })

    test("não cria um torneio com o nome maior que 200 caracteres", async ({ page }) => {
        await openTournaments(page)

        await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Torneio")
        await form.getByLabel("Nome").fill("a".repeat(201))
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form.getByText("Máximo de 200 caracteres")).toBeVisible()
    })

    test("mostra um torneio", async ({ page, data }) => {
        const tournament = await data.tournament()
        await openTournaments(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), tournament.name), "Ver")

        const view = page.getByRole("dialog")
        await expect(view).toContainText(tournament.id)
        await expect(view).toContainText(tournament.name)
        await expect(view).toContainText("Critérios de divisãoNenhum")
    })

    test("edita um torneio", async ({ page, data }) => {
        const tournament = await data.tournament()
        const newName = uniqueName("Torneio")
        await openTournaments(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), tournament.name), "Editar")
        const form = dialog(page, "Torneio")
        await expect(form.getByLabel("Nome")).toHaveValue(tournament.name)
        await form.getByLabel("Nome").fill(newName)
        await chooseOption(page, form.getByLabel("Formato"), "Todos contra todos")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Torneio salvo")
        await expect(rowWith(page.getByRole("tabpanel"), newName)).toContainText("Todos contra todos")
        await expect(rowWith(page.getByRole("tabpanel"), tournament.name)).toHaveCount(0)
    })

    test("deleta um torneio", async ({ page, data }) => {
        const tournament = await data.tournament()
        await openTournaments(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), tournament.name), "Deletar")
        await expect(dialog(page, "Deletar torneio?")).toContainText(tournament.name)
        await confirmDeletion(page, "Deletar torneio?")

        await expectSuccessNotification(page, "Torneio deletado")
        await expect(rowWith(page.getByRole("tabpanel"), tournament.name)).toHaveCount(0)
    })

    test("não deleta um torneio com competidores", async ({ page, data }) => {
        const competitor = await data.tournamentCompetitor()
        await openTournaments(page)

        await clickRowAction(rowWith(page.getByRole("tabpanel"), competitor.tournament.name), "Deletar")
        await confirmDeletion(page, "Deletar torneio?")

        await expect(dialog(page, "Deletar torneio?"))
            .toContainText("Não é possível deletar: existem outros registros ligados a este.")
    })

    test("não busca possíveis competidores de uma disputa de título", async ({ page, data }) => {
        const titleMatch = await data.tournament({ format: TournamentFormat.TITLE_MATCH })
        const singleElimination = await data.tournament()
        await openTournaments(page)

        const panel = page.getByRole("tabpanel")
        await expect(rowWith(panel, titleMatch.name).getByRole("button", { name: "Possíveis competidores" })).toHaveCount(0)
        await expect(rowWith(panel, singleElimination.name).getByRole("button", { name: "Possíveis competidores" })).toBeVisible()
    })
})
