import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { RequestClient } from "../common/common.service"
import { AssistantService, MAX_HISTORY } from "./assistant.service"
import type { Exchange } from "./assistant.type"

const fetchMock = vi.fn<typeof fetch>()

beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock)
})

afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
})

describe("AssistantService", () => {
    const assistantService = new AssistantService(new RequestClient("http://api.test", async () => "access-token"))

    it("sends the message with the last exchanges, oldest first, and answers the reply", async () => {
        fetchMock.mockResolvedValue(new Response(JSON.stringify({ reply: "Pronto" }), { status: 200 }))
        const history: Array<Exchange> = Array.from(
            { length: MAX_HISTORY + 2 },
            (_, i) => ({ message: `message ${i}`, reply: `reply ${i}` })
        )

        const reply = await assistantService.chat("Cadastre um aluno", history)

        expect(reply).toBe("Pronto")
        const [url, init] = fetchMock.mock.calls[0]
        expect(url).toBe("http://api.test/assistant")
        expect(JSON.parse(init?.body as string)).toEqual({
            message: "Cadastre um aluno",
            history: history.slice(2)
        })
    })
})
