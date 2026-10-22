import type { Locator, Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import type { Grading } from "../../src/grading/grading.type"
import type { Rank } from "../../src/rank/rank.type"
import { chooseOption, clickRowAction, confirmDeletion, dialog, expectSuccessNotification, rowWith } from "../support/ui"

// The candidates of the grading, in the modal opened from its row
async function openCandidates(page: Page, grading: Grading): Promise<Locator> {
    await page.goto("/gradings")
    await clickRowAction(rowWith(page.getByRole("tabpanel"), grading.name), "Candidatos")
    return dialog(page, `Candidatos - ${grading.name}`)
}

function rankOption(rank: Rank): string {
    return `${rank.name} - ${rank.category.name}`
}

test.describe("Candidatos de uma graduação", () => {
    test("inscreve um membro buscado pelo CPF", async ({ page, data }) => {
        const grading = await data.grading()
        const member = await data.member()
        const rank = grading.gradingPrices[0].rank
        const modal = await openCandidates(page, grading)

        await modal.getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Candidato")
        await form.getByLabel("CPF").fill(member.cpf)
        await page.getByRole("option", { name: member.name }).click()
        await chooseOption(page, form.getByLabel("Grau pretendido"), rankOption(rank))
        await chooseOption(page, form.getByLabel("Forma de pagamento"), "Pix")
        await chooseOption(page, form.getByLabel("Pago"), "Sim")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Candidato salvo")
        const row = rowWith(modal, member.name)
        await expect(row).toContainText(rankOption(rank))
        await expect(row).toContainText("Pix")
    })

    test("não inscreve duas vezes o mesmo membro", async ({ page, data }) => {
        const candidate = await data.gradingCandidate()
        const modal = await openCandidates(page, candidate.grading)

        await modal.getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Candidato")
        await form.getByLabel("CPF").fill(candidate.member.cpf)
        await page.getByRole("option", { name: candidate.member.name }).click()
        await chooseOption(page, form.getByLabel("Grau pretendido"), rankOption(candidate.rank))
        await chooseOption(page, form.getByLabel("Forma de pagamento"), "Pix")
        await chooseOption(page, form.getByLabel("Pago"), "Não")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form).toContainText("Este membro já está inscrito nesta graduação.")
    })

    test("mostra só os candidatos da graduação", async ({ page, data }) => {
        const candidate = await data.gradingCandidate()
        const candidateOfAnotherGrading = await data.gradingCandidate()
        const modal = await openCandidates(page, candidate.grading)

        await expect(rowWith(modal, candidate.member.name)).toBeVisible()
        await expect(rowWith(modal, candidateOfAnotherGrading.member.name)).toHaveCount(0)

        await clickRowAction(rowWith(modal, candidate.member.name), "Ver")
        await expect(page.getByRole("dialog").last()).toContainText(candidate.id)
    })

    test("edita o grau pretendido e o pagamento de um candidato", async ({ page, data }) => {
        const candidate = await data.gradingCandidate()
        const anotherRank = await data.rank()
        const modal = await openCandidates(page, candidate.grading)

        await clickRowAction(rowWith(modal, candidate.member.name), "Editar")
        const form = dialog(page, `Candidato - ${candidate.member.name}`)
        await chooseOption(page, form.getByLabel("Grau pretendido"), rankOption(anotherRank))
        await chooseOption(page, form.getByLabel("Pago"), "Sim")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Candidato salvo")
        const row = rowWith(modal, candidate.member.name)
        await expect(row).toContainText(rankOption(anotherRank))
        await expect(row).toContainText("Sim")
    })

    test("deleta um candidato", async ({ page, data }) => {
        const candidate = await data.gradingCandidate()
        const modal = await openCandidates(page, candidate.grading)

        await clickRowAction(rowWith(modal, candidate.member.name), "Deletar")
        await expect(dialog(page, "Deletar candidato?")).toContainText(candidate.member.name)
        await confirmDeletion(page, "Deletar candidato?")

        await expectSuccessNotification(page, "Candidato deletado")
        await expect(rowWith(modal, candidate.member.name)).toHaveCount(0)
    })
})
