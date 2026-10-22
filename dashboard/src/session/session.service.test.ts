// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from "vitest"
import { SessionService } from "./session.service"
import { Session } from "./session.type"

const userManager = vi.hoisted(() => ({
    getUser: vi.fn(),
    signinSilent: vi.fn(),
    signinRedirect: vi.fn(),
    signinRedirectCallback: vi.fn(),
    signoutRedirect: vi.fn()
}))

vi.mock("oidc-client-ts", async importOriginal => ({
    ...await importOriginal<typeof import("oidc-client-ts")>(),
    UserManager: vi.fn(function () {
        return userManager
    })
}))

function user(accessToken: string, expiresIn: number) {
    return { access_token: accessToken, expires_in: expiresIn }
}

describe("SessionService", () => {
    let sessionService: SessionService

    beforeEach(() => {
        vi.clearAllMocks()
        sessionService = new SessionService("https://auth.test", "dashboard")
    })

    it("uses the token while it is valid", async () => {
        userManager.getUser.mockResolvedValue(user("valid", 300))

        expect(await sessionService.getAccessToken()).toBe("valid")
        expect(userManager.signinSilent).not.toHaveBeenCalled()
    })

    it("refreshes a token about to expire", async () => {
        userManager.getUser.mockResolvedValue(user("expiring", 10))
        userManager.signinSilent.mockResolvedValue(user("refreshed", 300))

        expect(await sessionService.getAccessToken()).toBe("refreshed")
    })

    // With rotating refresh tokens, a second refresh at once would use a refresh token already spent
    it("refreshes once for the requests made at the same time", async () => {
        userManager.getUser.mockResolvedValue(user("expiring", 10))
        userManager.signinSilent.mockResolvedValue(user("refreshed", 300))

        const tokens = await Promise.all([
            sessionService.getAccessToken(),
            sessionService.getAccessToken(),
            sessionService.getAccessToken()
        ])

        expect(tokens).toEqual(["refreshed", "refreshed", "refreshed"])
        expect(userManager.signinSilent).toHaveBeenCalledTimes(1)
    })

    it("refreshes again once the previous refresh has finished", async () => {
        userManager.getUser.mockResolvedValue(user("expiring", 10))
        userManager.signinSilent.mockResolvedValue(user("refreshed", 300))

        await sessionService.getAccessToken()
        await sessionService.getAccessToken()

        expect(userManager.signinSilent).toHaveBeenCalledTimes(2)
    })

    it("goes to the login when the token cannot be refreshed", async () => {
        userManager.getUser.mockResolvedValue(null)
        userManager.signinSilent.mockRejectedValue(new Error("invalid_grant"))

        await expect(sessionService.getAccessToken()).rejects.toThrow("Session expired")
        expect(userManager.signinRedirect).toHaveBeenCalledTimes(1)
    })
})

describe("Session", () => {
    it("uses the preferred username and the name", () => {
        const session = Session.from({ sub: "sub", preferred_username: "joao", name: "João", email: "j@x.test" } as never)

        expect(session).toEqual(new Session("joao", "João"))
    })

    it("falls back to the e-mail and then to the subject", () => {
        expect(Session.from({ sub: "sub", email: "j@x.test" } as never)).toEqual(new Session("j@x.test", "j@x.test"))
        expect(Session.from({ sub: "sub" } as never)).toEqual(new Session("sub", "sub"))
    })
})
