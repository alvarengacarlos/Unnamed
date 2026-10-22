import type { Locator, Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import type { StudentEnrollment } from "../../src/student-enrollment/student-enrollment.type"
import { chooseOption, clickRowAction, confirmDeletion, dialog, expectSuccessNotification, rowWith } from "../support/ui"

// The payments of the student, from the enrollment in the clazz: Alunos > Matrículas > Pagamentos
async function openPayments(page: Page, studentEnrollment: StudentEnrollment): Promise<Locator> {
    const member = studentEnrollment.student.member
    await page.goto("/members")
    await page.getByRole("tab", { name: "Alunos" }).click()
    await clickRowAction(rowWith(page.getByRole("tabpanel"), member.name), "Matrículas")
    await clickRowAction(rowWith(dialog(page, "Matrículas"), studentEnrollment.clazz.name), "Pagamentos")
    return dialog(page, `Pagamentos - ${member.name} - ${studentEnrollment.clazz.name}`)
}

test.describe("Pagamentos das mensalidades", () => {
    test("lista as matrículas do aluno", async ({ page, data }) => {
        const studentEnrollment = await data.studentEnrollment()
        const member = studentEnrollment.student.member
        await page.goto("/members")
        await page.getByRole("tab", { name: "Alunos" }).click()

        await clickRowAction(rowWith(page.getByRole("tabpanel"), member.name), "Matrículas")

        const row = rowWith(dialog(page, "Matrículas"), studentEnrollment.clazz.name)
        await expect(row).toContainText("Judo")
        await expect(row).toContainText("18:00 - 19:00")
    })

    test("registra o pagamento de um mês", async ({ page, data }) => {
        const studentEnrollment = await data.studentEnrollment()
        const modal = await openPayments(page, studentEnrollment)

        await modal.getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Pagamento")
        await chooseOption(page, form.getByLabel("Mês"), "Março")
        await form.getByLabel("Ano").fill("2031")
        await chooseOption(page, form.getByLabel("Pago"), "Sim")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Pagamento salvo")
        await expect(rowWith(modal, "Março/2031")).toContainText("Sim")
    })

    test("não registra duas vezes o mesmo mês", async ({ page, data }) => {
        const tuitionPayment = await data.tuitionPayment({ month: 1, year: 2031 })
        const modal = await openPayments(page, tuitionPayment.studentEnrollment)

        await modal.getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Pagamento")
        await chooseOption(page, form.getByLabel("Mês"), "Janeiro")
        await form.getByLabel("Ano").fill("2031")
        await chooseOption(page, form.getByLabel("Pago"), "Sim")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form).toContainText("Já existe um pagamento desta matrícula neste mês.")
    })

    test("mostra os pagamentos do aluno em todas as turmas", async ({ page, data }) => {
        const studentEnrollment = await data.studentEnrollment()
        const anotherEnrollment = await data.studentEnrollment({ student: studentEnrollment.student })
        await data.tuitionPayment({ studentEnrollment, month: 1, year: 2031 })
        await data.tuitionPayment({ studentEnrollment: anotherEnrollment, month: 2, year: 2031 })
        // A payment of another student, which is not listed
        await data.tuitionPayment({ month: 3, year: 2031 })
        const modal = await openPayments(page, studentEnrollment)

        await expect(rowWith(modal, "Janeiro/2031")).toContainText(studentEnrollment.clazz.name)
        await expect(rowWith(modal, "Fevereiro/2031")).toContainText(anotherEnrollment.clazz.name)
        await expect(rowWith(modal, "Março/2031")).toHaveCount(0)
    })

    test("marca um pagamento como pago", async ({ page, data }) => {
        const tuitionPayment = await data.tuitionPayment({ month: 4, year: 2031 })
        const modal = await openPayments(page, tuitionPayment.studentEnrollment)

        await clickRowAction(rowWith(modal, "Abril/2031"), "Editar")
        const form = dialog(page, "Pagamento")
        await chooseOption(page, form.getByLabel("Pago"), "Sim")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Pagamento salvo")
        await expect(rowWith(modal, "Abril/2031")).toContainText("Sim")

        await clickRowAction(rowWith(modal, "Abril/2031"), "Ver")
        await expect(page.getByRole("dialog").last()).toContainText(tuitionPayment.id)
    })

    test("deleta um pagamento", async ({ page, data }) => {
        const tuitionPayment = await data.tuitionPayment({ month: 5, year: 2031 })
        const modal = await openPayments(page, tuitionPayment.studentEnrollment)

        await clickRowAction(rowWith(modal, "Maio/2031"), "Deletar")
        await expect(dialog(page, "Deletar pagamento?")).toContainText("Maio/2031")
        await confirmDeletion(page, "Deletar pagamento?")

        await expectSuccessNotification(page, "Pagamento deletado")
        await expect(rowWith(modal, "Maio/2031")).toHaveCount(0)
    })
})
