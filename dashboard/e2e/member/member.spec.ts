import type { Page } from "@playwright/test"
import { test, expect } from "../fixtures"
import { uniqueCpf, uniqueName } from "../support/unique"
import { chooseOption, clickRowAction, confirmDeletion, dialog, expectSuccessNotification, rowWith } from "../support/ui"

// Opened after the data of the test is created, so the page loads it
async function openMembers(page: Page): Promise<void> {
    await page.goto("/members")
    await page.getByTestId("members-tab").click()
}

test.describe("Membros", () => {
    test("cria um membro", async ({ page, data }) => {
        const rank = await data.rank()
        const name = uniqueName("Membro")
        const cpf = uniqueCpf()
        await openMembers(page)

        await page.getByTestId("new-member-button").click()
        // The fields are found by their labels, which do not change with the masks and the date picker
        const form = page.getByTestId("member-form")
        await form.getByLabel("Nome").fill(name)
        await form.getByLabel("CPF").fill(cpf)
        await form.getByLabel("Telefone").fill("(11) 91234-5678")
        await form.getByLabel("Endereço").fill("Rua A, 1")
        await chooseOption(page, form.getByLabel("Sexo"), "Masculino")
        await form.getByLabel("Peso").fill("80")
        await form.getByLabel("Altura").fill("1,8")
        await form.getByLabel("Data de nascimento").fill("01/01/2000")
        await chooseOption(page, form.getByLabel("PCD"), "Não")
        await chooseOption(page, form.getByLabel("Competidor"), "Sim")
        await chooseOption(page, form.getByLabel("Grau"), `${rank.name} - ${rank.category.name}`)
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Membro salvo")
        const row = rowWith(page.getByTestId("member-body-table"), name)
        await expect(row).toContainText(cpf)
        await expect(row).toContainText(rank.name)
        await expect(row).toContainText("Sim")
    })

    test("mostra no formulário os graus além da primeira página da API", async ({ page, data }) => {
        // More ranks than a page of the API (50). The listing is the newest first, so the first one created is past
        // the first page, even with other ranks in the organization.
        const category = await data.rankCategory()
        const firstRank = await data.rank({ order: 1, category })
        await Promise.all(Array.from({ length: 50 }, (_, i) => data.rank({ order: i + 2, category })))
        await openMembers(page)

        await page.getByTestId("new-member-button").click()
        const form = page.getByTestId("member-form")
        await chooseOption(page, form.getByLabel("Grau"), `${firstRank.name} - ${category.name}`)

        await expect(form.getByLabel("Grau")).toHaveValue(`${firstRank.name} - ${category.name}`)
    })

    test("não cria um membro com o CPF incompleto", async ({ page }) => {
        await openMembers(page)

        await page.getByTestId("new-member-button").click()
        const form = page.getByTestId("member-form")
        await form.getByLabel("CPF").fill("123.456")
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form.getByText("CPF incompleto")).toBeVisible()
    })

    test("não cria um membro com o CPF de outro", async ({ page, data }) => {
        const member = await data.member()
        await openMembers(page)

        await page.getByTestId("new-member-button").click()
        const form = page.getByTestId("member-form")
        await form.getByLabel("Nome").fill(uniqueName("Membro"))
        await form.getByLabel("CPF").fill(member.cpf)
        await form.getByLabel("Telefone").fill("(11) 91234-5678")
        await form.getByLabel("Endereço").fill("Rua A, 1")
        await chooseOption(page, form.getByLabel("Sexo"), "Feminino")
        await form.getByLabel("Peso").fill("60")
        await form.getByLabel("Altura").fill("1,6")
        await form.getByLabel("Data de nascimento").fill("01/01/2000")
        await chooseOption(page, form.getByLabel("PCD"), "Não")
        await chooseOption(page, form.getByLabel("Competidor"), "Não")
        await chooseOption(page, form.getByLabel("Grau"), `${member.rank.name} - ${member.rank.category.name}`)
        await form.getByRole("button", { name: "Salvar" }).click()

        await expect(form).toContainText("Já existe um membro com este CPF.")
    })

    test("mostra um membro", async ({ page, data }) => {
        const member = await data.member()
        await openMembers(page)

        await clickRowAction(rowWith(page.getByTestId("member-body-table"), member.name), "Ver")

        const view = page.getByTestId("member-view-modal")
        await expect(view).toContainText(member.id)
        await expect(view).toContainText(member.cpf)
        await expect(view).toContainText(member.phone)
        await expect(view).toContainText(member.rank.name)
    })

    test("edita um membro", async ({ page, data }) => {
        const member = await data.member()
        const newName = uniqueName("Membro")
        await openMembers(page)

        await clickRowAction(rowWith(page.getByTestId("member-body-table"), member.name), "Editar")
        const form = page.getByTestId("member-form")
        await expect(form.getByLabel("CPF")).toHaveValue(member.cpf)
        await form.getByLabel("Nome").fill(newName)
        await form.getByRole("button", { name: "Salvar" }).click()

        await expectSuccessNotification(page, "Membro salvo")
        const table = page.getByTestId("member-body-table")
        await expect(rowWith(table, newName)).toContainText(member.cpf)
        await expect(rowWith(table, member.name)).toHaveCount(0)
    })

    test("deleta um membro", async ({ page, data }) => {
        const member = await data.member()
        await openMembers(page)

        await clickRowAction(rowWith(page.getByTestId("member-body-table"), member.name), "Deletar")
        await expect(dialog(page, "Deletar membro?")).toContainText(member.name)
        await confirmDeletion(page, "Deletar membro?")

        await expectSuccessNotification(page, "Membro deletado")
        await expect(rowWith(page.getByTestId("member-body-table"), member.name)).toHaveCount(0)
    })

    test("não deleta um membro que é aluno", async ({ page, data }) => {
        const student = await data.student()
        await openMembers(page)

        await clickRowAction(rowWith(page.getByTestId("member-body-table"), student.member.name), "Deletar")
        await confirmDeletion(page, "Deletar membro?")

        await expect(dialog(page, "Deletar membro?"))
            .toContainText("Não é possível deletar: existem outros registros ligados a este.")
    })

    test("busca os membros pelo nome", async ({ page, data }) => {
        const prefix = uniqueName("Busca")
        const member = await data.member({ name: `${prefix} Carlos` })
        const anotherMember = await data.member({ name: `${prefix} Pedro` })
        await openMembers(page)

        // Part of the name, in another case
        await page.getByRole("textbox", { name: "Buscar" }).fill(`${prefix} carl`.toLowerCase())

        const table = page.getByTestId("member-body-table")
        await expect(rowWith(table, anotherMember.name)).toHaveCount(0)
        await expect(table.getByRole("row")).toHaveCount(1)
        await expect(rowWith(table, member.name)).toBeVisible()
    })

    test("busca um membro pelo CPF", async ({ page, data }) => {
        const member = await data.member()
        const anotherMember = await data.member()
        await openMembers(page)

        // Without the mask, as it is usually typed
        await page.getByRole("textbox", { name: "Buscar" }).fill(member.cpf.replace(/\D/g, ""))

        const table = page.getByTestId("member-body-table")
        await expect(rowWith(table, anotherMember.name)).toHaveCount(0)
        await expect(table.getByRole("row")).toHaveCount(1)
        await expect(rowWith(table, member.name)).toBeVisible()
    })

    test("mostra a mensagem da busca sem resultado", async ({ page, data }) => {
        // A member the search must not show
        await data.member()
        await openMembers(page)

        await page.getByRole("textbox", { name: "Buscar" }).fill(uniqueName("Ninguém"))

        await expect(page.getByTestId("member-body-table")).toHaveText("Nenhum registro cadastrado")
    })
})
