import type { Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import type { TestData } from "../support/test-data"
import type { Member } from "../../src/member/member.type"
import { uniqueName } from "../support/unique"
import { clickRowAction, confirmDeletion, dialog, expectSuccessNotification, rowWith } from "../support/ui"

// A student and a teacher are a role of a member, with the same screen
const roles = [
    {
        tab: "Alunos",
        title: "Aluno",
        saved: "Aluno salvo",
        deleted: "Aluno deletado",
        deleteTitle: "Deletar aluno?",
        exists: "Este membro já é aluno.",
        viewTestId: "student-view-modal",
        create: (data: TestData, member?: Member) => data.student({ member })
    },
    {
        tab: "Professores",
        title: "Professor",
        saved: "Professor salvo",
        deleted: "Professor deletado",
        deleteTitle: "Deletar professor?",
        exists: "Este membro já é professor.",
        viewTestId: "teacher-view-modal",
        create: (data: TestData, member?: Member) => data.teacher({ member })
    }
]

for (const role of roles) {
    test.describe(role.tab, () => {
        // Opened after the data of the test is created, so the page loads it
        async function openRole(page: Page): Promise<void> {
            await page.goto("/members")
            await page.getByRole("tab", { name: role.tab }).click()
        }

        test(`cria um ${role.title.toLowerCase()} de um membro buscado pelo CPF`, async ({ page, data }) => {
            const member = await data.member()
            await openRole(page)

            await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
            const form = dialog(page, role.title)
            await form.getByLabel("CPF").fill(member.cpf)
            await page.getByRole("option", { name: member.name }).click()
            await form.getByRole("button", { name: "Salvar" }).click()

            await expectSuccessNotification(page, role.saved)
            await expect(rowWith(page.getByRole("tabpanel"), member.name)).toContainText(member.cpf)
        })

        test(`não cria um ${role.title.toLowerCase()} de um membro que já é`, async ({ page, data }) => {
            const existing = await role.create(data)
            await openRole(page)

            await page.getByRole("tabpanel").getByRole("button", { name: "Novo" }).click()
            const form = dialog(page, role.title)
            await form.getByLabel("CPF").fill(existing.member.cpf)
            await page.getByRole("option", { name: existing.member.name }).click()
            await form.getByRole("button", { name: "Salvar" }).click()

            await expect(form).toContainText(role.exists)
        })

        test(`mostra um ${role.title.toLowerCase()}`, async ({ page, data }) => {
            const existing = await role.create(data)
            await openRole(page)

            await clickRowAction(rowWith(page.getByRole("tabpanel"), existing.member.name), "Ver")

            const view = page.getByTestId(role.viewTestId)
            await expect(view).toContainText(existing.id)
            await expect(view).toContainText(existing.member.cpf)
        })

        test(`deleta um ${role.title.toLowerCase()}, mas não o membro`, async ({ page, data }) => {
            const existing = await role.create(data)
            await openRole(page)

            await clickRowAction(rowWith(page.getByRole("tabpanel"), existing.member.name), "Deletar")
            await confirmDeletion(page, role.deleteTitle)

            await expectSuccessNotification(page, role.deleted)
            await expect(rowWith(page.getByRole("tabpanel"), existing.member.name)).toHaveCount(0)
            await page.getByRole("tab", { name: "Membros" }).click()
            await expect(rowWith(page.getByTestId("member-body-table"), existing.member.name)).toBeVisible()
        })

        test(`busca os ${role.tab.toLowerCase()} pelo nome e pelo CPF`, async ({ page, data }) => {
            const prefix = uniqueName("Busca")
            const existing = await role.create(data, await data.member({ name: `${prefix} Carlos` }))
            const another = await role.create(data, await data.member({ name: `${prefix} Pedro` }))
            await openRole(page)
            const panel = page.getByRole("tabpanel")
            const search = panel.getByRole("textbox", { name: "Buscar" })

            await search.fill(`${prefix} carl`.toLowerCase())
            await expect(rowWith(panel, another.member.name)).toHaveCount(0)
            await expect(rowWith(panel, existing.member.name)).toBeVisible()

            await search.fill(another.member.cpf)
            await expect(rowWith(panel, existing.member.name)).toHaveCount(0)
            await expect(rowWith(panel, another.member.name)).toBeVisible()
        })
    })
}
