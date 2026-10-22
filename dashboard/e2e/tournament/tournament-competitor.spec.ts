import type { Locator, Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import type { Tournament } from "../../src/tournament/tournament.type"
import { chooseOption, clickRowAction, confirmDeletion, dialog, expectSuccessNotification, rowWith } from "../support/ui"

// The competitors of the tournament, in the modal opened from its row
async function openCompetitors(page: Page, tournament: Tournament): Promise<Locator> {
    await page.goto("/tournaments")
    await clickRowAction(rowWith(page.getByRole("tabpanel"), tournament.name), "Competidores e lutas")
    const modal = dialog(page, `Competidores e lutas - ${tournament.name}`)
    await modal.getByRole("tab", { name: "Competidores" }).click()
    return modal.getByRole("tabpanel")
}

test.describe("Competidores de um torneio", () => {
    test("inscreve um membro buscado pelo CPF", async ({ page, data }) => {
        const tournament = await data.tournament()
        const member = await data.member()
        const panel = await openCompetitors(page, tournament)

        await panel.getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Competidor")
        await form.getByLabel("CPF").fill(member.cpf)
        await page.getByRole("option", { name: member.name }).click()
        await chooseOption(page, form.getByLabel("Forma de pagamento"), "Dinheiro")
        await chooseOption(page, form.getByLabel("Pago"), "Sim")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Competidor salvo")
        const row = rowWith(panel, member.name)
        await expect(row).toContainText("Dinheiro")
        await expect(row).toContainText("Sim")
    })

    test("não inscreve duas vezes o mesmo membro", async ({ page, data }) => {
        const competitor = await data.tournamentCompetitor()
        const panel = await openCompetitors(page, competitor.tournament)

        await panel.getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Competidor")
        await form.getByLabel("CPF").fill(competitor.member.cpf)
        await page.getByRole("option", { name: competitor.member.name }).click()
        await chooseOption(page, form.getByLabel("Forma de pagamento"), "Pix")
        await chooseOption(page, form.getByLabel("Pago"), "Não")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form).toContainText("Este membro já está inscrito neste torneio.")
    })

    test("mostra só os competidores do torneio", async ({ page, data }) => {
        const competitor = await data.tournamentCompetitor()
        const competitorOfAnotherTournament = await data.tournamentCompetitor()
        const panel = await openCompetitors(page, competitor.tournament)

        await expect(rowWith(panel, competitor.member.name)).toBeVisible()
        await expect(rowWith(panel, competitorOfAnotherTournament.member.name)).toHaveCount(0)

        await clickRowAction(rowWith(panel, competitor.member.name), "Ver")
        await expect(page.getByRole("dialog").last()).toContainText(competitor.id)
    })

    test("edita o pagamento de um competidor", async ({ page, data }) => {
        const competitor = await data.tournamentCompetitor()
        const panel = await openCompetitors(page, competitor.tournament)

        await clickRowAction(rowWith(panel, competitor.member.name), "Editar")
        const form = dialog(page, `Competidor - ${competitor.member.name}`)
        await chooseOption(page, form.getByLabel("Forma de pagamento"), "Cartão de crédito")
        await chooseOption(page, form.getByLabel("Pago"), "Não")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Competidor salvo")
        const row = rowWith(panel, competitor.member.name)
        await expect(row).toContainText("Cartão de crédito")
        await expect(row).toContainText("Não")
    })

    test("deleta um competidor", async ({ page, data }) => {
        const competitor = await data.tournamentCompetitor()
        const panel = await openCompetitors(page, competitor.tournament)

        await clickRowAction(rowWith(panel, competitor.member.name), "Deletar")
        await confirmDeletion(page, "Deletar competidor?")

        await expectSuccessNotification(page, "Competidor deletado")
        await expect(rowWith(panel, competitor.member.name)).toHaveCount(0)
    })

    test("não deleta um competidor de alguma luta", async ({ page, data }) => {
        const match = await data.tournamentMatch()
        const panel = await openCompetitors(page, match.tournament)

        await clickRowAction(rowWith(panel, match.competitorA.member.name), "Deletar")
        await confirmDeletion(page, "Deletar competidor?")

        await expect(dialog(page, "Deletar competidor?"))
            .toContainText("Não é possível deletar: existem outros registros ligados a este.")
    })
})
