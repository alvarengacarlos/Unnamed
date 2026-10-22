import type { Page, Route } from "@playwright/test"
import { mockOidcAuthority, mockOidcClientId } from "../config"

const authorityPath = new URL(mockOidcAuthority).pathname
const TOKEN_LIFETIME_IN_SECONDS = 60 * 60

export const mockUser = {
    username: "e2e",
    name: "Usuário E2E"
}

/**
 * Answers the login of the app (authorization code with PKCE) as an OpenID Connect provider would, without a login
 * page: the discovery points to the endpoints below, the authorization redirects straight back to the app, and the
 * token endpoint gives tokens of the user above. The app only decodes the tokens, so they need no valid signature, but
 * the id token must carry the nonce of the authorization, when there is one.
 */
export async function installFakeOidcProvider(page: Page): Promise<void> {
    let nonce: string | undefined

    await page.route(url => url.pathname === `${authorityPath}/.well-known/openid-configuration`, async route => {
        await route.fulfill({
            json: {
                issuer: mockOidcAuthority,
                authorization_endpoint: `${mockOidcAuthority}/authorize`,
                token_endpoint: `${mockOidcAuthority}/token`,
                end_session_endpoint: `${mockOidcAuthority}/logout`,
                jwks_uri: `${mockOidcAuthority}/jwks`,
                response_types_supported: ["code"],
                subject_types_supported: ["public"],
                id_token_signing_alg_values_supported: ["none"],
                code_challenge_methods_supported: ["S256"]
            }
        })
    })

    await page.route(url => url.pathname === `${authorityPath}/authorize`, async route => {
        const params = new URL(route.request().url()).searchParams
        nonce = params.get("nonce") ?? undefined
        const callback = new URLSearchParams({
            state: params.get("state") ?? "",
            code: "e2e"
        })
        await redirect(route, `${params.get("redirect_uri")}?${callback}`)
    })

    await page.route(url => url.pathname === `${authorityPath}/token`, async route => {
        await route.fulfill({
            json: {
                access_token: createToken({}),
                refresh_token: "e2e",
                id_token: createToken({ nonce }),
                token_type: "Bearer",
                expires_in: TOKEN_LIFETIME_IN_SECONDS
            }
        })
    })

    await page.route(url => url.pathname === `${authorityPath}/logout`, async route => {
        const params = new URL(route.request().url()).searchParams
        await redirect(route, params.get("post_logout_redirect_uri") ?? "/")
    })
}

// A page that goes to the URL, as a fulfilled 302 is not followed by every browser on a navigation
async function redirect(route: Route, url: string): Promise<void> {
    await route.fulfill({
        contentType: "text/html",
        body: `<script>location.replace(${JSON.stringify(url)})</script>`
    })
}

function createToken(claims: Record<string, unknown>): string {
    const now = Math.floor(Date.now() / 1000)
    const header = { alg: "none", typ: "JWT" }
    const payload = {
        iat: now,
        exp: now + TOKEN_LIFETIME_IN_SECONDS,
        iss: mockOidcAuthority,
        aud: mockOidcClientId,
        sub: "00000000-0000-4000-8000-0000000000e2",
        preferred_username: mockUser.username,
        name: mockUser.name,
        ...claims
    }
    return `${base64Url(header)}.${base64Url(payload)}.e2e`
}

function base64Url(value: object): string {
    return Buffer.from(JSON.stringify(value)).toString("base64url")
}
