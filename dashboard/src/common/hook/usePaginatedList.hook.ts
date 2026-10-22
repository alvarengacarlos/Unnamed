import { useState } from "react"
import { useDebouncedCallback } from "@mantine/hooks"
import type { Pagination } from "../common.type"
import { useLoadRequest, useOnMount } from "./useRequest.hook"

// The Pagination of Mantine starts at 1 and the page of the API at 0
const FIRST_PAGE = 1
const PAGE_SIZE = 50

/**
 * The list of a screen, a page at a time: loads the first page when the screen mounts, goes to the page chosen and
 * filters it. A reload after a deletion that emptied the last page shows the page before it.
 * loadPage receives the page of the API (from 0) and the filter, which is the text of the search unless another
 * initialFilter is given, and which a list without a filter ignores.
 */
export function usePaginatedList<T, F = string>(
    loadPage: (page: number, pageSize: number, filter: F) => Promise<Pagination<T>>,
    initialFilter: F = "" as F
) {
    const [page, setPage] = useState(FIRST_PAGE)
    const [filter, setFilter] = useState(initialFilter)
    const [pagination, setPagination] = useState<Pagination<T>>()
    const request = useLoadRequest()

    function load(pageToLoad = page, filterToLoad = filter) {
        request.run(
            () => loadPage(pageToLoad - 1, PAGE_SIZE, filterToLoad),
            pagination => {
                if (pagination.content.length === 0 && pageToLoad > FIRST_PAGE) {
                    load(pageToLoad - 1, filterToLoad)
                    return
                }

                setPagination(pagination)
                setPage(pageToLoad)
            }
        )
    }

    // Waits the typing to stop, so a request is not made at each letter
    const loadSearch = useDebouncedCallback((filterToLoad: F) => load(FIRST_PAGE, filterToLoad), 400)

    useOnMount(() => load(FIRST_PAGE))

    return {
        items: pagination?.content ?? [],
        page,
        totalPages: pagination?.totalPages ?? 0,
        filter,
        isLoading: request.isLoading,
        hasLoaded: request.hasLoaded,
        error: request.error,
        clearError: request.clearError,
        // The current page and filter again, such as after a record is saved or deleted
        reload: () => load(),
        goToPage: (pageToLoad: number) => load(pageToLoad),
        // A new filter starts at the first page
        changeFilter: (filterToLoad: F) => {
            setFilter(filterToLoad)
            load(FIRST_PAGE, filterToLoad)
        },
        // The same, for a text being typed
        changeSearch: (filterToLoad: F) => {
            setFilter(filterToLoad)
            loadSearch(filterToLoad)
        }
    }
}

export type PaginatedList<T> = Pick<ReturnType<typeof usePaginatedList<T>>, "page" | "totalPages" | "goToPage">
