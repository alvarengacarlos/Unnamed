import { describe, expect, it } from "vitest"
import { requiredText, TEXT_MAX_LENGTH } from "./common.schema"

describe("requiredText", () => {
    const schema = requiredText("Informe o nome")

    it("keeps the text without the spaces around it", () => {
        expect(schema.parse("  João  ")).toBe("João")
    })

    it("refuses an empty or blank text with the given message", () => {
        for (const text of ["", "   "]) {
            const result = schema.safeParse(text)

            expect(result.success).toBe(false)
            expect(result.error?.issues[0].message).toBe("Informe o nome")
        }
    })

    it("accepts the limit of the core-api", () => {
        expect(schema.safeParse("a".repeat(TEXT_MAX_LENGTH)).success).toBe(true)
    })

    it("refuses a text over the limit of the core-api", () => {
        const result = schema.safeParse("a".repeat(TEXT_MAX_LENGTH + 1))

        expect(result.success).toBe(false)
        expect(result.error?.issues[0].message).toBe(`Máximo de ${TEXT_MAX_LENGTH} caracteres`)
    })

    // The limit is checked after the trim, as the core-api receives the trimmed text
    it("does not count the spaces around the text", () => {
        expect(schema.safeParse(` ${"a".repeat(TEXT_MAX_LENGTH)} `).success).toBe(true)
    })
})
