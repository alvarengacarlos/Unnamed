import { test as base } from "@playwright/test"
import { isApiMocked } from "./support/config"
import { FakeCoreApi, installFakeCoreApi } from "./support/mock/fake-core-api"
import { installFakeOidcProvider } from "./support/mock/fake-oidc-provider"
import {
    FakeApiClient,
    RealApiClient,
    deleteCreatedResources,
    newAuthorizedRequest,
    trackCreatedResources,
    type ApiClient
} from "./support/api-client"
import { TestData } from "./support/test-data"

interface Fixtures {
    // The core-api in memory of the test; unused against the real backend
    fakeCoreApi: FakeCoreApi
    // What the test created on the real core-api, deleted at its end
    createdResources: Array<string>
    api: ApiClient
    data: TestData
}

/**
 * The tests are the same with and without the backend (E2E_MOCK_API): the page and the data of the test talk to the
 * fake core-api and OpenID Connect provider, or to the real ones.
 */
export const test = base.extend<Fixtures>({
    // eslint-disable-next-line no-empty-pattern -- Playwright requires the destructuring, even with no fixture
    fakeCoreApi: async ({}, use) => {
        await use(new FakeCoreApi())
    },

    createdResources: async ({ playwright }, use) => {
        const createdResources: Array<string> = []
        await use(createdResources)
        if (!isApiMocked) {
            await deleteCreatedResources(playwright, createdResources)
        }
    },

    page: async ({ page, fakeCoreApi, createdResources }, use) => {
        if (isApiMocked) {
            await installFakeOidcProvider(page)
            await installFakeCoreApi(page, fakeCoreApi)
            await use(page)
            return
        }

        const tracking: Array<Promise<void>> = []
        page.on("response", response => {
            // A response of a page already closed has no body; there is nothing to track then
            tracking.push(trackCreatedResources(response, createdResources).catch(() => {}))
        })
        await use(page)
        await Promise.all(tracking)
    },

    api: async ({ playwright, fakeCoreApi, createdResources }, use) => {
        if (isApiMocked) {
            await use(new FakeApiClient(fakeCoreApi))
            return
        }

        const request = await newAuthorizedRequest(playwright)
        await use(new RealApiClient(request, createdResources))
        await request.dispose()
    },

    data: async ({ api }, use) => {
        await use(new TestData(api))
    }
})

export { expect } from "@playwright/test"
