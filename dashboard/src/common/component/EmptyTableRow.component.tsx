import { Table, Text } from "@mantine/core"

interface Props {
    colSpan: number
}

export default function EmptyTableRow(props: Props) {
    return (
        <Table.Tr>
            <Table.Td colSpan={props.colSpan}>
                <Text c="dimmed" ta="center">Nenhum registro cadastrado</Text>
            </Table.Td>
        </Table.Tr>
    )
}
