import { expect, type Locator, type Page } from "@playwright/test"

// The row of the table that shows the text, such as the unique name of a record
export function rowWith(table: Locator, text: string): Locator {
    return table.getByRole("row").filter({ hasText: text })
}

// The modal open on top, as the ones of a form or of a deletion
export function dialog(page: Page, title: string): Locator {
    return page.getByRole("dialog").filter({ has: page.getByRole("heading", { name: title, exact: true }) })
}

// An option of a Select, whose list opens outside the form
export async function chooseOption(page: Page, select: Locator, option: string): Promise<void> {
    await select.click()
    await page.getByRole("option", { name: option, exact: true }).click()
}

// Clicks the action of the row (Ver, Editar, Deletar, …), the buttons with only an icon
export async function clickRowAction(row: Locator, action: string): Promise<void> {
    await row.getByRole("button", { name: action, exact: true }).click()
}

export async function confirmDeletion(page: Page, title: string): Promise<void> {
    await dialog(page, title).getByRole("button", { name: "Deletar" }).click()
}

export async function expectSuccessNotification(page: Page, message: string): Promise<void> {
    await expect(page.getByRole("alert").filter({ hasText: message })).toBeVisible()
}

const monthFormat = new Intl.DateTimeFormat("pt-BR", { month: "long" })

/**
 * Chooses the date and the time on a DateTimePicker, which takes no typing: the day on its calendar and the time on its
 * fields. The date as 2030-02-01, at or after the month the calendar opens on (the one of the value, or of today), and
 * the time as 09:30.
 */
export async function chooseDateTime(page: Page, picker: Locator, date: string, time: string): Promise<void> {
    await picker.click()
    const dropdown = page.locator(`#${await picker.getAttribute("aria-controls")}`)
    const [year, month, day] = date.split("-").map(Number)
    // The name of each day of the calendar, as 1 fevereiro 2030
    const dayButton = dropdown.getByRole("button", {
        name: `${day} ${monthFormat.format(new Date(year, month - 1, 1))} ${year}`,
        exact: true
    })
    while (!await dayButton.isVisible()) {
        await dropdown.locator("button[data-direction='next']").click()
    }
    await dayButton.click()

    const [hour, minute] = time.split(":")
    await dropdown.getByRole("spinbutton").nth(0).pressSequentially(hour)
    await dropdown.getByRole("spinbutton").nth(1).pressSequentially(minute)
    // The button that confirms the date and the time, the last of the dropdown
    await dropdown.getByRole("button").last().click()
    await expect(dropdown).toBeHidden()
}

// A date some months from today, as 2026-12-10, so the calendar reaches it in a few clicks
export function dateInMonths(months: number, day: number): string {
    const today = new Date()
    const date = new Date(today.getFullYear(), today.getMonth() + months, day)
    const pad = (value: number) => String(value).padStart(2, "0")
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
