import path from "path"

/**
 * E2E_MOCK_API=false runs the tests against the real core-api and OpenID Connect provider, configured in the .env. Any
 * other value, or none, runs them against a core-api and a provider simulated in the browser, without any backend.
 */
export const isApiMocked = process.env.E2E_MOCK_API !== "false"

// With the mocks the app runs on a port of its own, so it is never confused with a dev server of the real backend
const MOCK_APP_PORT = 5174
export const mockAppUrl = `http://localhost:${MOCK_APP_PORT}`

// On the origin of the app, so the requests have no CORS. Every request to them is answered by the tests.
export const mockApiBaseUrl = `${mockAppUrl}/mock-api`
export const mockOidcAuthority = `${mockAppUrl}/mock-oidc`
export const mockOidcClientId = "dashboard"

export const appUrl = isApiMocked ? mockAppUrl : (process.env.E2E_BASE_URL ?? "http://localhost:5173")
export const apiBaseUrl = isApiMocked ? mockApiBaseUrl : requireEnv("VITE_API_BASE_URL")

// The session of the provider opened by the global setup, reused by all the tests against the real backend
export const sessionStatePath = path.resolve("e2e/.auth/session.json")

export function requireEnv(name: string): string {
    const value = process.env[name]
    if (!value) {
        throw new Error(`The environment variable ${name} is required when E2E_MOCK_API=false`)
    }

    return value
}
