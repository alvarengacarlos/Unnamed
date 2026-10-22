import { describe, expect, it } from "vitest"
import {
    convertDateToView,
    convertErrorToView,
    convertLocalDateTimeToForm,
    convertLocalDateTimeToService,
    convertMonthToService,
    convertSearchToCpf,
    convertSearchToMemberFilter,
    convertServiceToMonth
} from "./common.converter"
import { HttpError, type ProblemDetail } from "./common.service"
import { Month } from "./common.type"

describe("month", () => {
    // The API reads and writes a month as its number, January being 1
    it("is the number of the month in the API", () => {
        expect(convertMonthToService(Month.JANUARY)).toBe(1)
        expect(convertMonthToService(Month.OCTOBER)).toBe(10)
        expect(convertMonthToService(Month.DECEMBER)).toBe(12)
    })

    it("goes and comes back the same for every month", () => {
        for (const month of Object.values(Month)) {
            expect(convertServiceToMonth(convertMonthToService(month))).toBe(month)
        }
    })

    it("refuses a number that is not a month", () => {
        expect(() => convertServiceToMonth(0)).toThrow()
        expect(() => convertServiceToMonth(13)).toThrow()
    })
})

describe("dates", () => {
    it("shows a date of the API as dd/mm/yyyy", () => {
        expect(convertDateToView("2026-10-21")).toBe("21/10/2026")
    })

    it("converts a date and time between the API and the DateTimePicker", () => {
        expect(convertLocalDateTimeToForm("2026-10-21T10:00:00")).toBe("2026-10-21 10:00:00")
        expect(convertLocalDateTimeToService("2026-10-21 10:00:00")).toBe("2026-10-21T10:00:00")
    })
})

describe("convertErrorToView", () => {
    function httpError(status: number, problemDetail: ProblemDetail): HttpError {
        return new HttpError(status, problemDetail)
    }

    it("tells that the server was not reached when the request failed before an answer", () => {
        expect(convertErrorToView(new TypeError("Failed to fetch")))
            .toBe("Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.")
    })

    it("uses the message of a known problem type before the generic ones", () => {
        expect(convertErrorToView(httpError(409, { type: "urn:unnamed:problem:member-exists" })))
            .toBe("Já existe um membro com este CPF.")
    })

    it("names the invalid fields", () => {
        const error = httpError(400, {
            type: "urn:unnamed:problem:invalid-input",
            errors: [{ field: "name", message: "is invalid" }, { field: "cpf", message: "is invalid" }]
        })

        expect(convertErrorToView(error)).toBe("Dados inválidos nos campos: name, cpf.")
    })

    it("tells that the input is invalid when no field is named", () => {
        expect(convertErrorToView(httpError(400, { type: "urn:unnamed:problem:invalid-input" })))
            .toBe("Dados inválidos.")
    })

    it("tells that the record exists for a new -exists type", () => {
        expect(convertErrorToView(httpError(409, { type: "urn:unnamed:problem:something-new-exists" })))
            .toBe("Este registro já existe.")
    })

    it("tells that the record was not found for any -not-found type", () => {
        expect(convertErrorToView(httpError(404, { type: "urn:unnamed:problem:member-not-found" })))
            .toBe("Registro não encontrado. Ele pode ter sido deletado; recarregue a página.")
    })

    it("tells that the session expired on a 401", () => {
        expect(convertErrorToView(httpError(401, { type: "urn:unnamed:problem:invalid-token" })))
            .toBe("Sua sessão expirou. Recarregue a página para entrar novamente.")
    })

    it("tells that the server failed on a 5xx, even without a problem detail", () => {
        expect(convertErrorToView(httpError(502, {}))).toBe("Erro no servidor. Tente novamente mais tarde.")
    })

    it("falls back to a generic message", () => {
        expect(convertErrorToView(httpError(403, { type: "urn:unnamed:problem:invalid-organization-claim" })))
            .toBe("Não foi possível concluir a operação.")
    })
})

describe("convertSearchToCpf", () => {
    it.each([
        ["123.456.789-01"],
        ["12345678901"],
        [" 123 456 789 01 "]
    ])("formats the complete CPF %j", search => {
        expect(convertSearchToCpf(search)).toBe("123.456.789-01")
    })

    it.each([
        ["123.456.789"],
        ["123456789012"],
        ["João 12345678901"],
        [""]
    ])("is null for %j, which is not a complete CPF", search => {
        expect(convertSearchToCpf(search)).toBeNull()
    })
})

describe("convertSearchToMemberFilter", () => {
    it("searches by the CPF when the text is a complete CPF", () => {
        expect(convertSearchToMemberFilter("12345678901")).toEqual({ cpf: "123.456.789-01" })
    })

    it("searches by the name, without the spaces around it", () => {
        expect(convertSearchToMemberFilter("  João ")).toEqual({ name: "João" })
    })

    it("lists everything when nothing is typed", () => {
        expect(convertSearchToMemberFilter("   ")).toEqual({})
    })
})
