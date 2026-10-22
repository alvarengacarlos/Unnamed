import { Loader, Select, type ComboboxItem, type SelectProps } from "@mantine/core"
import { useState } from "react"
import { convertSearchToCpf } from "../common.converter"
import { useLoadRequest } from "../hook/useRequest.hook"
import RequestErrorAlert from "./RequestErrorAlert.component"

// Props of the Select, such as the ones of getInputProps of a form. The options come from the search.
type Props = Omit<SelectProps, "data" | "searchable" | "onSearchChange"> & {
    // The options of a complete CPF, such as the member or the student with it
    search: (cpf: string) => Promise<Array<ComboboxItem>>
}

export default function SearchableCpfSelect({ search, ...props }: Props) {
    const [options, setOptions] = useState<Array<ComboboxItem>>([])
    const searchRequest = useLoadRequest()

    return (
        <>
            <Select
                withAsterisk
                label="CPF"
                placeholder="000.000.000-00"
                {...props}
                searchable
                data={options}
                rightSection={searchRequest.isLoading ? <Loader size="xs"/> : undefined}
                onSearchChange={(value) => {
                    // Also called with the label of the option chosen, which is not a CPF
                    const cpf = convertSearchToCpf(value)
                    if (!cpf) {
                        return
                    }

                    // Replaced even when empty, so an option of a previous search is never left to be chosen
                    searchRequest.run(() => search(cpf), setOptions)
                }}
            />
            <RequestErrorAlert error={searchRequest.error} onClose={searchRequest.clearError}/>
        </>
    )
}
