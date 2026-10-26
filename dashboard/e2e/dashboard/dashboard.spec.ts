import { test, expect } from "../fixtures"
import type { PaginationOutput } from "../../src/common/common.type"

test.describe("Dashboard", () => {
    test("mostra o total de membros, alunos e professores", async ({ page, api, data }) => {
        await data.student()
        await data.teacher()
        // Against the real backend the totals have the records of other runs too, so they are read from the API
        const total = async (path: string) =>
            (await api.get<PaginationOutput<unknown>>(`${path}?page=0&pageSize=1`)).page.totalElements
        const totals = {
            "Membros": await total("/members"),
            "Alunos": await total("/students"),
            "Professores": await total("/teachers")
        }

        await page.goto("/")

        // The card of each total, with its label and its number
        const main = page.getByRole("main")
        for (const [label, value] of Object.entries(totals)) {
            await expect(main.getByText(label, { exact: true }).locator(".."))
                .toHaveText(`${label}${value.toLocaleString("pt-BR")}`)
        }
    })

    test("abre cada página pelo menu", async ({ page }) => {
        const pages = [
            { link: "Mensalidades", path: "/tuitions", tab: "Mensalidades" },
            { link: "Graus", path: "/ranks", tab: "Categorias" },
            { link: "Membros", path: "/members", tab: "Membros" },
            { link: "Turmas", path: "/clazzes", tab: "Turmas" },
            { link: "Graduações", path: "/gradings", tab: "Graduações" },
            { link: "Torneios", path: "/tournaments", tab: "Torneios" },
            { link: "Assistente", path: "/assistant", heading: "Assistente" },
            { link: "Dashboard", path: "/", heading: "Dashboard" }
        ]
        await page.goto("/")

        for (const { link, path, tab, heading } of pages) {
            await page.getByRole("navigation").getByRole("link", { name: link }).click()

            await expect(page).toHaveURL(path)
            if (tab) {
                await expect(page.getByRole("tab", { name: tab, exact: true })).toBeVisible()
            } else {
                await expect(page.getByRole("heading", { name: heading })).toBeVisible()
            }
        }
    })
})
