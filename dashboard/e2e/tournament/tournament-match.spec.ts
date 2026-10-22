import type { Locator, Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import type { Tournament } from "../../src/tournament/tournament.type"
import { chooseDateTime, chooseOption, clickRowAction, confirmDeletion, dateInMonths, dialog, expectSuccessNotification, rowWith } from "../support/ui"
import { convertDateToView } from "../../src/common/common.converter"

// The matches of the tournament, in the modal opened from its row
async function openMatches(page: Page, tournament: Tournament): Promise<Locator> {
    await page.goto("/tournaments")
    await clickRowAction(rowWith(page.getByRole("tabpanel"), tournament.name), "Competidores e lutas")
    const modal = dialog(page, `Competidores e lutas - ${tournament.name}`)
    await modal.getByRole("tab", { name: "Lutas" }).click()
    return modal.getByRole("tabpanel")
}

test.describe("Lutas de um torneio", () => {
    test("cria uma luta entre dois competidores do torneio", async ({ page, data }) => {
        const tournament = await data.tournament()
        const a = await data.tournamentCompetitor({ tournament })
        const b = await data.tournamentCompetitor({ tournament })
        const day = dateInMonths(1, 12)
        const panel = await openMatches(page, tournament)

        await panel.getByRole("button", { name: "Novo", exact: true }).click()
        const form = dialog(page, "Luta")
        await chooseOption(page, form.getByLabel("Competidor A"), a.member.name)
        await chooseOption(page, form.getByLabel("Competidor B"), b.member.name)
        await chooseDateTime(page, form.getByLabel("Data e hora"), day, "10:30")
        await form.getByLabel("Local").fill("Tatame 2")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Luta salva")
        const row = rowWith(panel, a.member.name)
        await expect(row).toContainText(b.member.name)
        await expect(row).toContainText(`${convertDateToView(day)}, 10:30`)
        await expect(row).toContainText("Pendente")
    })

    test("não cria uma luta de um competidor contra ele mesmo", async ({ page, data }) => {
        const competitor = await data.tournamentCompetitor()
        const panel = await openMatches(page, competitor.tournament)

        await panel.getByRole("button", { name: "Novo", exact: true }).click()
        const form = dialog(page, "Luta")
        await chooseOption(page, form.getByLabel("Competidor A"), competitor.member.name)
        await chooseOption(page, form.getByLabel("Competidor B"), competitor.member.name)
        await chooseDateTime(page, form.getByLabel("Data e hora"), dateInMonths(1, 12), "10:30")
        await form.getByLabel("Local").fill("Tatame 1")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form.getByText("Os competidores precisam ser diferentes")).toBeVisible()
    })

    test("não cria uma luta que já existe com os competidores trocados", async ({ page, data }) => {
        const match = await data.tournamentMatch()
        const panel = await openMatches(page, match.tournament)

        await panel.getByRole("button", { name: "Novo", exact: true }).click()
        const form = dialog(page, "Luta")
        await chooseOption(page, form.getByLabel("Competidor A"), match.competitorB.member.name)
        await chooseOption(page, form.getByLabel("Competidor B"), match.competitorA.member.name)
        await chooseDateTime(page, form.getByLabel("Data e hora"), dateInMonths(1, 12), "10:30")
        await form.getByLabel("Local").fill("Tatame 1")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form).toContainText("Esta luta já existe.")
    })

    test("registra o resultado de uma luta", async ({ page, data }) => {
        const match = await data.tournamentMatch()
        const panel = await openMatches(page, match.tournament)
        const nameA = match.competitorA.member.name

        await clickRowAction(rowWith(panel, nameA), "Editar")
        const form = dialog(page, `${nameA} x ${match.competitorB.member.name}`)
        await chooseOption(page, form.getByLabel("Resultado"), `Vitória de ${nameA}`)
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Luta salva")
        await expect(rowWith(panel, nameA)).toContainText(`Vitória de ${nameA}`)

        await clickRowAction(rowWith(panel, nameA), "Ver")
        await expect(page.getByRole("dialog").last()).toContainText(match.id)
    })

    test("deleta uma luta", async ({ page, data }) => {
        const match = await data.tournamentMatch()
        const panel = await openMatches(page, match.tournament)

        await clickRowAction(rowWith(panel, match.competitorA.member.name), "Deletar")
        await expect(dialog(page, "Deletar luta?")).toContainText(match.competitorB.member.name)
        await confirmDeletion(page, "Deletar luta?")

        await expectSuccessNotification(page, "Luta deletada")
        await expect(rowWith(panel, match.competitorA.member.name)).toHaveCount(0)
    })

    test("cria lutas em lote", async ({ page, data }) => {
        const tournament = await data.tournament()
        const [a, b, c] = await Promise.all([
            data.tournamentCompetitor({ tournament }),
            data.tournamentCompetitor({ tournament }),
            data.tournamentCompetitor({ tournament })
        ])
        const panel = await openMatches(page, tournament)

        await panel.getByRole("button", { name: "Novo em lote" }).click()
        const form = dialog(page, `Novas lutas em lote - ${tournament.name}`)
        const first = form.getByRole("group", { name: "Luta 1" })
        await chooseOption(page, first.getByLabel("Competidor A"), a.member.name)
        await chooseOption(page, first.getByLabel("Competidor B"), b.member.name)
        await chooseDateTime(page, first.getByLabel("Data e hora"), dateInMonths(1, 12), "10:30")
        await first.getByLabel("Local").fill("Tatame 1")
        // The new match starts with the date and the place of the last one
        await form.getByRole("button", { name: "Adicionar luta" }).click()
        const second = form.getByRole("group", { name: "Luta 2" })
        await expect(second.getByLabel("Local")).toHaveValue("Tatame 1")
        await chooseOption(page, second.getByLabel("Competidor A"), a.member.name)
        await chooseOption(page, second.getByLabel("Competidor B"), c.member.name)
        await form.getByRole("button", { name: "Salvar 2 luta(s)" }).click()

        await expectSuccessNotification(page, "2 luta(s) criada(s)")
        await expect(rowWith(panel, b.member.name)).toContainText(a.member.name)
        await expect(rowWith(panel, c.member.name)).toContainText(a.member.name)
    })

    test("não cria lutas em lote com uma luta repetida", async ({ page, data }) => {
        const tournament = await data.tournament()
        const [a, b] = await Promise.all([
            data.tournamentCompetitor({ tournament }),
            data.tournamentCompetitor({ tournament })
        ])
        const panel = await openMatches(page, tournament)

        await panel.getByRole("button", { name: "Novo em lote" }).click()
        const form = dialog(page, `Novas lutas em lote - ${tournament.name}`)
        const first = form.getByRole("group", { name: "Luta 1" })
        await chooseOption(page, first.getByLabel("Competidor A"), a.member.name)
        await chooseOption(page, first.getByLabel("Competidor B"), b.member.name)
        await chooseDateTime(page, first.getByLabel("Data e hora"), dateInMonths(1, 12), "10:30")
        await first.getByLabel("Local").fill("Tatame 1")
        await form.getByRole("button", { name: "Adicionar luta" }).click()
        // B x A is the same match as A x B
        const second = form.getByRole("group", { name: "Luta 2" })
        await chooseOption(page, second.getByLabel("Competidor A"), b.member.name)
        await chooseOption(page, second.getByLabel("Competidor B"), a.member.name)
        await form.getByRole("button", { name: "Salvar 2 luta(s)" }).click()

        await expect(form).toContainText("Há itens repetidos na lista.")
        await expect(rowWith(panel, a.member.name)).toHaveCount(0)
    })
})
