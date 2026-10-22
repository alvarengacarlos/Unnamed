import { Button, Group, Modal, Table, TextInput } from "@mantine/core"
import { useDisclosure } from "@mantine/hooks";
import z from "zod/v4";
import { schemaResolver, useForm } from "@mantine/form";
import { useState, type ReactNode } from "react";
import { TrashIcon, EyeIcon, MagnifyingGlassIcon } from "@phosphor-icons/react";
import type { Member } from "./member.type";
import type { LabelValue, Pagination } from "../common/common.type";
import ViewObjectModal from "../common/component/ViewObjectModal.component";
import { convertBooleanToView, convertSearchToMemberFilter } from "../common/common.converter";
import SearchableCpfSelect from "../common/component/SearchableCpfSelect.component";
import { searchMemberOptionsByCpf } from "./member.search";
import { useRequest } from "../common/hook/useRequest.hook";
import { usePaginatedList } from "../common/hook/usePaginatedList.hook";
import { showSuccessNotification } from "../common/common.notification";
import RequestErrorAlert from "../common/component/RequestErrorAlert.component";
import RequestLoader from "../common/component/RequestLoader.component";
import ActionButton from "../common/component/ActionButton.component";
import DeleteConfirmModal from "../common/component/DeleteConfirmModal.component";
import EmptyTableRow from "../common/component/EmptyTableRow.component";
import ListPagination from "../common/component/ListPagination.component";

// The columns of the member and the ones of the actions Ver and Deletar
const COLUMN_COUNT = 6

const memberRoleFormSchema = z.object({
    memberId: z.uuid({error: 'Membro é obrigatório'})
})

// A role of a member, such as a student or a teacher
interface MemberRole {
    readonly id: string
    readonly member: Member
}

interface MemberRoleService<T> {
    getAll(page: number, pageSize: number, filters: { name?: string, cpf?: string }): Promise<Pagination<T>>
    create(input: MemberRoleForm): Promise<T>
    deleteById(id: string): Promise<T>
}

// An action of a row besides Ver and Deletar, shown before them
interface MemberRoleAction<T> {
    readonly label: string
    readonly icon: ReactNode
    readonly onClick: (role: T) => void
}

interface Props<T extends MemberRole> {
    service: MemberRoleService<T>
    // The name of the role, as Aluno, in the title of the form and in the messages
    title: string
    convertToView: (role: T) => Array<LabelValue>
    viewTestId: string
    actions?: Array<MemberRoleAction<T>>
}

/**
 * The tab of a role of a member, the same for the students and the teachers: lists and searches the members with the
 * role, gives the role to a member and takes it away, without deleting the member.
 */
export default function MemberRoleTab<T extends MemberRole>(props: Props<T>) {
    const actions = props.actions ?? []
    const titleInText = props.title.toLowerCase()

    const [isViewModalOpen, { open: openViewModal, close: closeViewModal }] = useDisclosure(false)
    const [isCreateModalOpen, { open: openCreateModal, close: closeCreateModal }] = useDisclosure(false)
    const [isDeleteModalOpen, { open: openDeleteModal, close: closeDeleteModal }] = useDisclosure(false)

    const form = useForm<MemberRoleForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(memberRoleFormSchema)
    })
    const [roleToBeDeleted, setRoleToBeDeleted] = useState<T | null>()
    const [role, setRole] = useState<T>()

    const list = usePaginatedList((page, pageSize, search) =>
        props.service.getAll(page, pageSize, convertSearchToMemberFilter(search)))
    const createRequest = useRequest()
    const deleteRequest = useRequest()

    const rows = list.items.map(r =>
        <Table.Tr key={r.id}>
            <Table.Td>{r.member.name}</Table.Td>
            <Table.Td>{r.member.cpf}</Table.Td>
            <Table.Td>{r.member.rank.name}</Table.Td>
            <Table.Td>{convertBooleanToView(r.member.isCompetitor)}</Table.Td>
            {actions.map(action =>
                <Table.Td key={action.label}>
                    <ActionButton label={action.label} icon={action.icon} onClick={() => action.onClick(r)}/>
                </Table.Td>
            )}
            <Table.Td>
                <ActionButton label="Ver" icon={<EyeIcon/>} onClick={() => {
                    setRole(r)
                    openViewModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setRoleToBeDeleted(r)
                    openDeleteModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    )

    return (
        <>
            <Group justify="space-between">
                <Button variant="filled" onClick={() => {
                    form.reset()
                    openCreateModal()
                }}>Novo</Button>
                <TextInput
                    aria-label="Buscar"
                    placeholder="Buscar por nome ou CPF"
                    leftSection={<MagnifyingGlassIcon/>}
                    value={list.filter}
                    onChange={(event) => list.changeSearch(event.currentTarget.value)}
                />
            </Group>

            <ViewObjectModal
                dataTestId={props.viewTestId}
                isViewObjectModalOpen={isViewModalOpen}
                closeViewObjectModal={closeViewModal}
                labelValue={role ? props.convertToView(role) : []}
            />

            <Modal
                opened={isCreateModalOpen}
                onClose={() => {
                    closeCreateModal()
                    createRequest.clearError()
                }}
                title={props.title}
                centered
            >
                <form onSubmit={form.onSubmit((values) => createRequest.run(async () => {
                    await props.service.create(values)

                    closeCreateModal()
                    showSuccessNotification(`${props.title} salvo`)
                    list.reload()
                }))}>
                    <RequestErrorAlert error={createRequest.error} onClose={createRequest.clearError}/>
                    <SearchableCpfSelect
                        search={searchMemberOptionsByCpf}
                        key={form.key('memberId')}
                        {...form.getInputProps("memberId")}
                    />
                    <Button variant="filled" type="submit" loading={createRequest.isLoading}>Salvar</Button>
                </form>
            </Modal>

            <DeleteConfirmModal
                opened={isDeleteModalOpen}
                onClose={closeDeleteModal}
                title={`Deletar ${titleInText}?`}
                isLoading={deleteRequest.isLoading}
                error={deleteRequest.error}
                onCloseError={deleteRequest.clearError}
                onConfirm={() => deleteRequest.run(async () => {
                    if (!roleToBeDeleted) {
                        throw new Error("roleToBeDeleted is empty")
                    }

                    await props.service.deleteById(roleToBeDeleted.id)
                    setRoleToBeDeleted(null)
                    closeDeleteModal()
                    showSuccessNotification(`${props.title} deletado`)
                    list.reload()
                })}
            >
                Tem certeza que deseja deletar o {titleInText} {roleToBeDeleted?.member.name}?
            </DeleteConfirmModal>

            <RequestErrorAlert error={list.error} onClose={list.clearError}/>
            <RequestLoader isLoading={list.isLoading} hasLoaded={list.hasLoaded}>
                <Table striped mt="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Nome</Table.Th>
                            <Table.Th>CPF</Table.Th>
                            <Table.Th>Grau</Table.Th>
                            <Table.Th>Competidor</Table.Th>
                            {actions.map(action => <Table.Th key={action.label}></Table.Th>)}
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                        {rows.length > 0 ? rows : <EmptyTableRow colSpan={COLUMN_COUNT + actions.length}/>}
                    </Table.Tbody>
                </Table>
            </RequestLoader>

            <ListPagination list={list}/>
        </>
    )
}

interface MemberRoleForm {
    memberId: string
}
