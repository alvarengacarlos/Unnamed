import { test, expect } from "../fixtures"
import { isApiMocked } from "../support/config"
import type { Page } from "@playwright/test"

// Against the real backend the model answers, which takes seconds
const REPLY_TIMEOUT = 60_000

test.describe("Assistente", () => {
    test("mostra a mensagem enviada e a resposta do assistente", async ({ page, fakeCoreApi }) => {
        const message = "Quantos alunos temos?"
        await page.goto("/assistant")

        await page.getByLabel("Mensagem").fill(message)
        await page.getByRole("button", { name: "Enviar" }).click()

        await expectExchange(page, message)
        await expect(page.getByLabel("Mensagem")).toHaveValue("")
        if (isApiMocked) {
            expect(fakeCoreApi.requests.filter(r => r.path === "/assistant").map(r => r.body))
                .toEqual([{ message, history: [] }])
        }
    })

    test("envia as trocas anteriores com a mensagem", async ({ page, fakeCoreApi }) => {
        test.skip(!isApiMocked, "The reply of the real model varies, and the history carries it")
        await page.goto("/assistant")
        const input = page.getByLabel("Mensagem")

        await input.fill("Cadastre um aluno")
        await input.press("Enter")
        await expect(page.getByRole("article", { name: "Assistente" })).toHaveCount(1)
        await input.fill("O nome é João")
        await input.press("Enter")
        await expect(page.getByRole("article", { name: "Assistente" })).toHaveCount(2)

        const [first, second] = fakeCoreApi.requests.filter(r => r.path === "/assistant").map(r => r.body)
        expect(first).toEqual({ message: "Cadastre um aluno", history: [] })
        expect(second).toEqual({
            message: "O nome é João",
            history: [{ message: "Cadastre um aluno", reply: "**Mensagem recebida:** Cadastre um aluno" }]
        })
    })

    test("começa uma nova conversa sem as trocas anteriores", async ({ page, fakeCoreApi }) => {
        await page.goto("/assistant")
        const input = page.getByLabel("Mensagem")
        const newConversation = page.getByRole("button", { name: "Nova conversa" })
        const conversation = page.getByRole("log", { name: "Conversa" })
        await expect(newConversation).toBeDisabled()

        await input.fill("Cadastre um aluno")
        await input.press("Enter")
        await expect(conversation.getByRole("article", { name: "Assistente" })).toHaveCount(1, { timeout: REPLY_TIMEOUT })
        await newConversation.click()

        await expect(conversation.getByRole("article")).toHaveCount(0)
        await expect(newConversation).toBeDisabled()
        await input.fill("Quantos alunos temos?")
        await input.press("Enter")
        await expectExchange(page, "Quantos alunos temos?")
        if (isApiMocked) {
            expect(fakeCoreApi.requests.filter(r => r.path === "/assistant").map(r => r.body).at(-1))
                .toEqual({ message: "Quantos alunos temos?", history: [] })
        }
    })

    test("envia com Enter e quebra a linha com Shift+Enter", async ({ page }) => {
        await page.goto("/assistant")
        const input = page.getByLabel("Mensagem")

        await input.fill("Quantos alunos")
        await input.press("Shift+Enter")
        await input.pressSequentially("temos?")
        await expect(page.getByRole("log", { name: "Conversa" }).getByRole("article")).toHaveCount(0)
        await input.press("Enter")

        await expectExchange(page, "Quantos alunos\ntemos?")
    })

    test("não envia uma mensagem em branco", async ({ page }) => {
        await page.goto("/assistant")

        await page.getByLabel("Mensagem").fill("   ")

        await expect(page.getByRole("button", { name: "Enviar" })).toBeDisabled()
    })
})

// The message in the conversation, followed by a reply; with the fake core-api the reply repeats the message
async function expectExchange(page: Page, message: string): Promise<void> {
    const conversation = page.getByRole("log", { name: "Conversa" })
    await expect(conversation.getByRole("article", { name: "Você" })).toHaveText(message)
    const reply = conversation.getByRole("article", { name: "Assistente" })
    await expect(reply).not.toBeEmpty({ timeout: REPLY_TIMEOUT })
    if (isApiMocked) {
        await expect(reply).toContainText(message)
        // The reply is Markdown, shown formatted: the bold of the fake core-api without its asterisks
        await expect(reply).toContainText("Mensagem recebida:")
        await expect(reply).not.toContainText("**")
    }
}
