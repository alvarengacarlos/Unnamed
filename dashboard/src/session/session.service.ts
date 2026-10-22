import { InMemoryWebStorage, UserManager, WebStorageStateStore, type User } from "oidc-client-ts"
import { Session } from "./session.type"

// A token about to expire is refreshed before the request, so it does not expire on the way to the API
const MIN_TOKEN_VALIDITY_IN_SECONDS = 30

/**
 * The session of the user, opened by the login page of any OpenID Connect provider (authorization code with PKCE),
 * found by the discovery of its issuer. The tokens live only in memory: a reload goes back to the provider, which
 * answers without asking for the login again while its own session lasts.
 */
export class SessionService {
    private readonly userManager: UserManager
    // One refresh at a time: with rotating refresh tokens, a second one at once would use a refresh token already spent
    private refreshing: Promise<User> | null = null

    constructor(authority: string, clientId: string) {
        this.userManager = new UserManager({
            authority,
            client_id: clientId,
            // A fixed URI, as most providers only accept the exact URIs registered for the client
            redirect_uri: `${window.location.origin}/`,
            post_logout_redirect_uri: window.location.origin,
            scope: "openid profile",
            userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
            // The token is refreshed by getAccessToken, when a request needs it
            automaticSilentRenew: false
        })
    }

    async start(): Promise<Session> {
        console.info("Starting session")
        if (!this.isLoginCallback()) {
            await this.login()
            // The page is going to the login page of the provider, which redirects back to the app
            return new Promise(() => {})
        }

        const user = await this.userManager.signinRedirectCallback()
        // Back to the page the user was on, without the code and the state of the login in the URL
        window.history.replaceState(null, "", typeof user.state === "string" ? user.state : "/")
        console.info("Success")
        return Session.from(user.profile)
    }

    async getAccessToken(): Promise<string> {
        const user = await this.userManager.getUser()
        if (user && (user.expires_in ?? Infinity) > MIN_TOKEN_VALIDITY_IN_SECONDS) {
            return user.access_token
        }

        try {
            return (await this.refresh()).access_token
        } catch (e) {
            console.warn("Cannot refresh the token, the session has expired. Error: " + e)
            await this.login()
            throw new Error("Session expired", { cause: e })
        }
    }

    async end(): Promise<void> {
        console.info("Ending session")
        await this.userManager.signoutRedirect()
    }

    // By the refresh token, when the provider gave one
    private refresh(): Promise<User> {
        this.refreshing ??= this.userManager.signinSilent()
            .then(user => {
                if (!user) {
                    throw new Error("No user after the refresh")
                }
                return user
            })
            .finally(() => this.refreshing = null)
        return this.refreshing
    }

    private login(): Promise<void> {
        const { pathname, search, hash } = window.location
        return this.userManager.signinRedirect({ state: pathname + search + hash })
    }

    // The provider redirects back to the redirect_uri with the state of the login and its code, or an error
    private isLoginCallback(): boolean {
        const params = new URLSearchParams(window.location.search)
        return window.location.pathname === "/" && params.has("state") && (params.has("code") || params.has("error"))
    }
}
