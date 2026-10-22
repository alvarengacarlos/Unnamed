import type { Locator, Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import type { Clazz } from "../../src/clazz/clazz.type"
import { chooseOption, clickRowAction, confirmDeletion, dialog, expectSuccessNotification, rowWith } from "../support/ui"

// The enrollments of the clazz, in the modal opened from its row
async function openEnrollments(page: Page, clazz: Clazz): Promise<Locator> {
    await page.goto("/clazzes")
    await clickRowAction(rowWith(page.getByRole("tabpanel"), clazz.name), "Matrículas")
    return dialog(page, `Matrículas - ${clazz.name}`)
}

test.describe("Matrículas de uma turma", () => {
    test("matricula um aluno buscado pelo CPF", async ({ page, data }) => {
        const clazz = await data.clazz()
        const student = await data.student()
        const tuition = await data.tuition()
        const modal = await openEnrollments(page, clazz)

        await modal.getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Matrícula")
        await form.getByLabel("CPF").fill(student.member.cpf)
        await page.getByRole("option", { name: student.member.name }).click()
        await chooseOption(page, form.getByLabel("Mensalidade"), tuition.name)
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Matrícula salva")
        const row = rowWith(modal, student.member.name)
        await expect(row).toContainText(student.member.cpf)
        await expect(row).toContainText("Sim")
    })

    test("não matricula duas vezes o mesmo aluno na turma", async ({ page, data }) => {
        const studentEnrollment = await data.studentEnrollment()
        const modal = await openEnrollments(page, studentEnrollment.clazz)

        await modal.getByRole("button", { name: "Novo" }).click()
        const form = dialog(page, "Matrícula")
        await form.getByLabel("CPF").fill(studentEnrollment.student.member.cpf)
        await page.getByRole("option", { name: studentEnrollment.student.member.name }).click()
        await chooseOption(page, form.getByLabel("Mensalidade"), studentEnrollment.tuition.name)
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form).toContainText("Este aluno já está matriculado nesta turma.")
    })

    test("mostra só as matrículas da turma", async ({ page, data }) => {
        const studentEnrollment = await data.studentEnrollment()
        const enrollmentOfAnotherClazz = await data.studentEnrollment()
        const modal = await openEnrollments(page, studentEnrollment.clazz)

        await expect(rowWith(modal, studentEnrollment.student.member.name)).toBeVisible()
        await expect(rowWith(modal, enrollmentOfAnotherClazz.student.member.name)).toHaveCount(0)
    })

    test("desativa uma matrícula", async ({ page, data }) => {
        const studentEnrollment = await data.studentEnrollment()
        const modal = await openEnrollments(page, studentEnrollment.clazz)

        await clickRowAction(rowWith(modal, studentEnrollment.student.member.name), "Editar")
        const form = dialog(page, "Matrícula")
        await chooseOption(page, form.getByLabel("Ativa"), "Não")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Matrícula salva")
        await expect(rowWith(modal, studentEnrollment.student.member.name)).toContainText("Não")
    })

    test("deleta uma matrícula", async ({ page, data }) => {
        const studentEnrollment = await data.studentEnrollment()
        const modal = await openEnrollments(page, studentEnrollment.clazz)

        await clickRowAction(rowWith(modal, studentEnrollment.student.member.name), "Deletar")
        await expect(dialog(page, "Deletar matrícula?")).toContainText(studentEnrollment.student.member.name)
        await confirmDeletion(page, "Deletar matrícula?")

        await expectSuccessNotification(page, "Matrícula deletada")
        await expect(rowWith(modal, studentEnrollment.student.member.name)).toHaveCount(0)
    })

    test("não deleta uma matrícula com pagamentos", async ({ page, data }) => {
        const tuitionPayment = await data.tuitionPayment()
        const studentEnrollment = tuitionPayment.studentEnrollment
        const modal = await openEnrollments(page, studentEnrollment.clazz)

        await clickRowAction(rowWith(modal, studentEnrollment.student.member.name), "Deletar")
        await confirmDeletion(page, "Deletar matrícula?")

        await expect(dialog(page, "Deletar matrícula?"))
            .toContainText("Não é possível deletar: existem outros registros ligados a este.")
    })
})
