import { Pagination } from "@mantine/core"
import type { PaginatedList } from "../hook/usePaginatedList.hook"

// The pages of a list, shown only when it has more than one
export default function ListPagination<T>(props: { list: PaginatedList<T> }) {
    if (props.list.totalPages <= 1) {
        return null
    }

    return (
        <Pagination
            mt="md"
            value={props.list.page}
            total={props.list.totalPages}
            onChange={props.list.goToPage}
        />
    )
}
