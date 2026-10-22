import type { APIRequestContext, PlaywrightWorkerArgs, Response } from "@playwright/test"
import { apiBaseUrl, appUrl, sessionStatePath } from "./config"
import type { FakeCoreApi } from "./mock/fake-core-api"
import type { ProblemDetail } from "../../src/common/common.service"

type Playwright = PlaywrightWorkerArgs["playwright"]

/**
 * The core-api the tests use to prepare their data: the fake one in memory or the real one. The same calls, so the
 * data of a test is the same in both.
 */
export interface ApiClient {
    get<O>(path: string): Promise<O>
    post<O>(path: string, body: unknown): Promise<O>
    // A request the API must refuse: its status and problem detail
    refused(method: "GET" | "POST" | "DELETE", path: string, body?: unknown): Promise<RefusedRequest>
}

export interface RefusedRequest {
    readonly status: number
    readonly problemDetail: ProblemDetail
}

export class FakeApiClient implements ApiClient {
    constructor(private readonly fakeCoreApi: FakeCoreApi) {}

    async get<O>(path: string): Promise<O> {
        const [pathname, query] = path.split("?")
        const response = this.fakeCoreApi.handle({ method: "GET", path: pathname, query: new URLSearchParams(query), body: undefined })
        if (response.status >= 400) {
            throw new Error(`GET ${path} failed: ${JSON.stringify(response.body)}`)
        }
        return response.body as O
    }

    async post<O>(path: string, body: unknown): Promise<O> {
        const response = this.fakeCoreApi.handle({ method: "POST", path, query: new URLSearchParams(), body })
        if (response.status >= 400) {
            throw new Error(`POST ${path} failed: ${JSON.stringify(response.body)}`)
        }
        return response.body as O
    }

    async refused(method: "GET" | "POST" | "DELETE", path: string, body?: unknown): Promise<RefusedRequest> {
        const [pathname, query] = path.split("?")
        const response = this.fakeCoreApi.handle({ method, path: pathname, query: new URLSearchParams(query), body })
        if (response.status < 400) {
            throw new Error(`${method} ${path} succeeded: ${JSON.stringify(response.body)}`)
        }
        return { status: response.status, problemDetail: response.body as ProblemDetail }
    }
}

export class RealApiClient implements ApiClient {
    constructor(
        private readonly request: APIRequestContext,
        // What the test created, to delete at its end
        private readonly createdResources: Array<string>
    ) {}

    async get<O>(path: string): Promise<O> {
        const response = await this.request.get(apiBaseUrl + path)
        if (!response.ok()) {
            throw new Error(`GET ${path} failed: ${response.status()} ${await response.text()}`)
        }
        return await response.json() as O
    }

    async post<O>(path: string, body: unknown): Promise<O> {
        const response = await this.request.post(apiBaseUrl + path, { data: body })
        if (!response.ok()) {
            throw new Error(`POST ${path} failed: ${response.status()} ${await response.text()}`)
        }
        const output = await response.json()
        this.createdResources.push(`${path}/${output.id}`)
        return output as O
    }

    async refused(method: "GET" | "POST" | "DELETE", path: string, body?: unknown): Promise<RefusedRequest> {
        const response = await this.request.fetch(apiBaseUrl + path, { method, data: body })
        if (response.ok()) {
            throw new Error(`${method} ${path} succeeded: ${await response.text()}`)
        }
        return { status: response.status(), problemDetail: await response.json() }
    }
}

// A token about to expire is fetched again, so it does not expire during a test
const MIN_TOKEN_VALIDITY_IN_SECONDS = 60

let cachedAccessToken: { value: string, expiresAt: number } | null = null

/**
 * A token of the user of the tests, taken from a request of the app to the API: the app opens on the session of the
 * provider kept by the global setup, so it needs no grant other than the one of the app.
 */
export async function fetchAccessToken(playwright: Playwright): Promise<string> {
    const now = Date.now() / 1000
    if (cachedAccessToken && cachedAccessToken.expiresAt - MIN_TOKEN_VALIDITY_IN_SECONDS > now) {
        return cachedAccessToken.value
    }

    const browser = await playwright.firefox.launch()
    try {
        const page = await browser.newPage({ baseURL: appUrl, storageState: sessionStatePath })
        // The home page asks the API for its numbers
        const request = page.waitForRequest(request =>
            request.url().startsWith(apiBaseUrl) && request.headers()["authorization"] !== undefined
        )
        await page.goto("/")
        const value = (await request).headers()["authorization"].replace(/^Bearer /, "")
        const { exp } = JSON.parse(Buffer.from(value.split(".")[1], "base64url").toString())
        cachedAccessToken = { value, expiresAt: exp }
        return value
    } finally {
        await browser.close()
    }
}

export async function newAuthorizedRequest(playwright: Playwright): Promise<APIRequestContext> {
    return await playwright.request.newContext({
        extraHTTPHeaders: { Authorization: `Bearer ${await fetchAccessToken(playwright)}` }
    })
}

// What the app created on the real core-api, from its responses: a POST answered with 201, of one record or a bulk
export async function trackCreatedResources(response: Response, createdResources: Array<string>): Promise<void> {
    const request = response.request()
    if (request.method() !== "POST" || response.status() !== 201 || !response.url().startsWith(apiBaseUrl)) {
        return
    }

    const path = pathOfApi(response.url()).replace(/\/bulk$/, "")
    const output = await response.json()
    for (const created of Array.isArray(output) ? output : [output]) {
        createdResources.push(`${path}/${created.id}`)
    }
}

// The newest first, so a record is deleted before the ones it references. The ones already deleted are ignored.
export async function deleteCreatedResources(playwright: Playwright, createdResources: Array<string>): Promise<void> {
    if (createdResources.length === 0) {
        return
    }

    const request = await newAuthorizedRequest(playwright)
    const failures: Array<string> = []
    try {
        // All of them are tried, so a failure does not leave the others behind
        for (const path of [...createdResources].reverse()) {
            try {
                const response = await request.delete(apiBaseUrl + path)
                if (!response.ok() && response.status() !== 404) {
                    failures.push(`${path}: ${response.status()} ${await response.text()}`)
                }
            } catch (e) {
                failures.push(`${path}: ${e}`)
            }
        }
    } finally {
        await request.dispose()
    }

    if (failures.length > 0) {
        throw new Error(`Cannot delete what the test created:\n${failures.join("\n")}`)
    }
}

// The path of a URL of the API without its base URL, as /members/{id}, whether the base URL has a path or not
export function pathOfApi(url: string): string {
    const basePath = new URL(apiBaseUrl).pathname.replace(/\/$/, "")
    return new URL(url).pathname.slice(basePath.length)
}
