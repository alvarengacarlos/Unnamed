import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { HttpError, RequestClient, ResourceService } from "./common.service"
import type { PaginationOutput } from "./common.type"

const BASE_URL = "http://api.test"
const TOKEN = "access-token"

function answer(status: number, body: unknown): Response {
    return new Response(typeof body === "string" ? body : JSON.stringify(body), { status })
}

function page<O>(content: Array<O>, number: number, totalPages: number): PaginationOutput<O> {
    return { content, page: { number, size: 50, totalElements: totalPages * content.length, totalPages } }
}

// The error a request throws
async function rejectionOf(request: Promise<unknown>): Promise<HttpError> {
    try {
        await request
    } catch (e) {
        return e as HttpError
    }
    throw new Error("The request did not fail")
}

const fetchMock = vi.fn<typeof fetch>()

beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock)
})

afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
})

describe("RequestClient", () => {
    const requestClient = new RequestClient(BASE_URL, async () => TOKEN)

    it("sends the token and the body as JSON", async () => {
        fetchMock.mockResolvedValue(answer(200, { id: "1" }))

        const output = await requestClient.post("/members", { name: "João" })

        expect(output).toEqual({ id: "1" })
        const [url, init] = fetchMock.mock.calls[0]
        expect(url).toBe(`${BASE_URL}/members`)
        expect(init?.method).toBe("POST")
        expect(init?.body).toBe(JSON.stringify({ name: "João" }))
        expect(new Headers(init?.headers).get("Authorization")).toBe(`Bearer ${TOKEN}`)
    })

    it("sends the query params", async () => {
        fetchMock.mockResolvedValue(answer(200, {}))

        await requestClient.get("/members", new Map([["name", "João Silva"], ["page", "0"]]))

        expect(fetchMock.mock.calls[0][0]).toBe(`${BASE_URL}/members?name=Jo%C3%A3o+Silva&page=0`)
    })

    it("throws the problem detail of an error", async () => {
        const problemDetail = { type: "urn:unnamed:problem:member-exists", detail: "Member exists" }
        fetchMock.mockResolvedValue(answer(409, problemDetail))

        const error = await rejectionOf(requestClient.post("/members", {}))

        expect(error).toBeInstanceOf(HttpError)
        expect(error.status).toBe(409)
        expect(error.problemDetail).toEqual(problemDetail)
    })

    // A proxy in the way may answer with HTML
    it("throws the status of an error whose body is not JSON", async () => {
        fetchMock.mockResolvedValue(answer(502, "<html>Bad Gateway</html>"))

        const error = await rejectionOf(requestClient.get("/members"))

        expect(error).toBeInstanceOf(HttpError)
        expect(error.status).toBe(502)
        expect(error.problemDetail).toEqual({})
    })
})

describe("ResourceService", () => {
    const service = new ResourceService<string, { id: string }, unknown, { name?: string, cpf?: string }>(
        new RequestClient(BASE_URL, async () => TOKEN),
        "/members",
        output => output.id
    )

    it("leaves out the filters that are not set", async () => {
        fetchMock.mockResolvedValue(answer(200, page([], 0, 0)))

        await service.getAll(2, 10, { name: "João", cpf: undefined })

        expect(fetchMock.mock.calls[0][0]).toBe(`${BASE_URL}/members?page=2&pageSize=10&name=Jo%C3%A3o`)
    })

    it("reads every page of the listing", async () => {
        fetchMock.mockImplementation(async input => {
            const pageNumber = Number(new URL(String(input)).searchParams.get("page"))
            return answer(200, page([{ id: `member-${pageNumber}` }], pageNumber, 3))
        })

        const all = await service.getAllPages()

        expect(all).toEqual(["member-0", "member-1", "member-2"])
        expect(fetchMock).toHaveBeenCalledTimes(3)
    })

    it("reads only the first page when there is one", async () => {
        fetchMock.mockResolvedValue(answer(200, page([{ id: "member-0" }], 0, 1)))

        expect(await service.getAllPages()).toEqual(["member-0"])
        expect(fetchMock).toHaveBeenCalledTimes(1)
    })
})
