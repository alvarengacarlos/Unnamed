import type { Locator, Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import type { Tournament } from "../../src/tournament/tournament.type"
import { chooseOption, clickRowAction, dialog, expectSuccessNotification, rowWith } from "../support/ui"

// The members who meet the criteria of the tournament, in the modal opened from its row
async function openPossibleCompetitors(page: Page, tournament: Tournament): Promise<Locator> {
    await page.goto("/tournaments")
    await clickRowAction(rowWith(page.getByRole("tabpanel"), tournament.name), "Possíveis competidores")
    return dialog(page, `Possíveis competidores - ${tournament.name}`)
}

test.describe("Possíveis competidores de um torneio", () => {
    test("lista os competidores do grau do torneio", async ({ page, data }) => {
        const rank = await data.rank()
        const tournament = await data.tournament({ rank })
        const competitor = await data.member({ rank })
        const notCompetitor = await data.member({ rank, isCompetitor: false })
        const ofAnotherRank = await data.member()
        const modal = await openPossibleCompetitors(page, tournament)

        await expect(rowWith(modal, competitor.name)).toContainText(competitor.cpf)
        await expect(rowWith(modal, notCompetitor.name)).toHaveCount(0)
        await expect(rowWith(modal, ofAnotherRank.name)).toHaveCount(0)
    })

    test("inscreve os membros selecionados como competidores", async ({ page, data }) => {
        const rank = await data.rank()
        const tournament = await data.tournament({ rank })
        const first = await data.member({ rank })
        const second = await data.member({ rank })
        const modal = await openPossibleCompetitors(page, tournament)

        await modal.getByRole("checkbox", { name: "Selecionar todos" }).check()
        await chooseOption(page, modal.getByLabel("Forma de pagamento"), "Pix")
        await chooseOption(page, modal.getByLabel("Pago"), "Sim")
        await modal.getByRole("button", { name: "Inscrever selecionados (2)" }).click()

        await expectSuccessNotification(page, "2 competidor(es) inscrito(s)")
        await page.goto("/tournaments")
        await clickRowAction(rowWith(page.getByRole("tabpanel"), tournament.name), "Competidores e lutas")
        const competitors = dialog(page, `Competidores e lutas - ${tournament.name}`).getByRole("tabpanel")
        await expect(rowWith(competitors, first.name)).toContainText("Pix")
        await expect(rowWith(competitors, second.name)).toContainText("Pix")
    })

    test("não inscreve um membro que já é competidor do torneio", async ({ page, data }) => {
        const rank = await data.rank()
        const tournament = await data.tournament({ rank })
        const member = await data.member({ rank })
        await data.tournamentCompetitor({ tournament, member })
        const modal = await openPossibleCompetitors(page, tournament)

        await modal.getByRole("checkbox", { name: `Selecionar ${member.name}` }).check()
        await chooseOption(page, modal.getByLabel("Forma de pagamento"), "Pix")
        await chooseOption(page, modal.getByLabel("Pago"), "Sim")
        await modal.getByRole("button", { name: "Inscrever selecionados (1)" }).click()

        await expect(modal).toContainText("Este membro já está inscrito neste torneio.")
    })
})
