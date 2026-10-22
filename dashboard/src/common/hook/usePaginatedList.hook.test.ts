// @vitest-environment jsdom
import { act, renderHook, waitFor } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"
import { Pagination } from "../common.type"
import { usePaginatedList } from "./usePaginatedList.hook"

describe("usePaginatedList", () => {
    it("loads the first page when it mounts, as page 0 of the API", async () => {
        const loadPage = vi.fn(async () => new Pagination(["a"], 1, 1))

        const { result } = renderHook(() => usePaginatedList(loadPage))

        await waitFor(() => expect(result.current.items).toEqual(["a"]))
        expect(loadPage).toHaveBeenCalledExactlyOnceWith(0, 50, "")
        expect(result.current.page).toBe(1)
    })

    it("shows the page before when a reload finds the last page empty, as after its last record is deleted", async () => {
        // Three pages; the third has lost its only record
        let pagesWithRecords = 3
        const loadPage = vi.fn(async (page: number) => page < pagesWithRecords
            ? new Pagination([`record of page ${page}`], pagesWithRecords, pagesWithRecords)
            : new Pagination<string>([], pagesWithRecords, pagesWithRecords))
        const { result } = renderHook(() => usePaginatedList(loadPage))
        await waitFor(() => expect(result.current.hasLoaded).toBe(true))
        act(() => result.current.goToPage(3))
        await waitFor(() => expect(result.current.page).toBe(3))

        pagesWithRecords = 2
        act(() => result.current.reload())

        await waitFor(() => expect(result.current.items).toEqual(["record of page 1"]))
        expect(result.current.page).toBe(2)
    })

    it("goes back to the first page with a new filter", async () => {
        const loadPage = vi.fn(async () => new Pagination(["a"], 3, 3))
        const { result } = renderHook(() => usePaginatedList(loadPage))
        await waitFor(() => expect(result.current.hasLoaded).toBe(true))
        act(() => result.current.goToPage(3))
        await waitFor(() => expect(result.current.page).toBe(3))

        act(() => result.current.changeFilter("João"))

        await waitFor(() => expect(loadPage).toHaveBeenLastCalledWith(0, 50, "João"))
        await waitFor(() => expect(result.current.page).toBe(1))
        expect(result.current.filter).toBe("João")
    })
})
