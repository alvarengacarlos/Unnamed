import { firefox, type FullConfig } from "@playwright/test"
import { isApiMocked, requireEnv, sessionStatePath } from "./support/config"

/**
 * Against the real backend: logs in once on the page of the OpenID Connect provider and keeps its session, which the
 * tests reuse, so the provider sends them back to the app without asking for the login again.
 */
export default async function globalSetup(config: FullConfig): Promise<void> {
    if (isApiMocked) {
        return
    }

    const baseURL = config.projects[0].use.baseURL as string
    const browser = await firefox.launch()
    try {
        const page = await browser.newPage({ baseURL })
        await page.goto("/")
        // Any login page: the visible text field takes the username and the password field the password, each sent
        // with Enter. The password is on the same page or, when the provider asks for the username first, on the next.
        const username = page.locator('input:is([type="text"], [type="email"], :not([type])):visible').first()
        const password = page.locator('input[type="password"]:visible').first()
        await username.fill(requireEnv("E2E_USERNAME"))
        if (!await password.isVisible()) {
            await username.press("Enter")
        }
        await password.fill(requireEnv("E2E_PASSWORD"))
        await password.press("Enter")
        await page.getByRole("button", { name: "Sair" }).waitFor()
        await page.context().storageState({ path: sessionStatePath })
    } finally {
        await browser.close()
    }
}
