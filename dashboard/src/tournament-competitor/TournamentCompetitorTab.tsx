import { Button, Modal, Select, Table } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { schemaResolver, useForm } from "@mantine/form";
import { useState } from "react";
import z from "zod/v4";
import { EyeIcon, PencilIcon, TrashIcon } from "@phosphor-icons/react";
import { tournamentCompetitorService } from "../instance";
import type { TournamentCompetitor } from "./tournament-competitor.type";
import type { Tournament } from "../tournament/tournament.type";
import { PaymentMethod } from "../common/common.type";
import { booleanOptions, convertBooleanToView, convertPaymentMethodToView, paymentMethodOptions } from "../common/common.converter";
import { convertTournamentCompetitorToView } from "./tournament-competitor.converter";
import { useRequest } from "../common/hook/useRequest.hook";
import { usePaginatedList } from "../common/hook/usePaginatedList.hook";
import { showSuccessNotification } from "../common/common.notification";
import ViewObjectModal from "../common/component/ViewObjectModal.component";
import RequestErrorAlert from "../common/component/RequestErrorAlert.component";
import RequestLoader from "../common/component/RequestLoader.component";
import ActionButton from "../common/component/ActionButton.component";
import DeleteConfirmModal from "../common/component/DeleteConfirmModal.component";
import EmptyTableRow from "../common/component/EmptyTableRow.component";
import ListPagination from "../common/component/ListPagination.component";
import SearchableCpfSelect from "../common/component/SearchableCpfSelect.component";
import { searchMemberOptionsByCpf } from "../member/member.search";

const updateTournamentCompetitorFormSchema = z.object({
    paymentMethod: z.enum(PaymentMethod, { error: 'Forma de pagamento é obrigatório' }),
    isPaid: z.boolean({ error: 'Pago é obrigatório' })
})

// The member is chosen only at the creation
const createTournamentCompetitorFormSchema = updateTournamentCompetitorFormSchema.extend({
    memberId: z.uuid({ error: 'Membro é obrigatório' })
})

interface Props {
    // The tournament whose competitors are managed
    tournament: Tournament
}

export default function TournamentCompetitorTab(props: Props) {
    const [isViewTournamentCompetitorModalOpen, { open: openViewTournamentCompetitorModal, close: closeViewTournamentCompetitorModal }] = useDisclosure(false)
    const [isCreateTournamentCompetitorModalOpen, { open: openCreateTournamentCompetitorModal, close: closeCreateTournamentCompetitorModal }] = useDisclosure(false)
    const [isUpdateTournamentCompetitorModalOpen, { open: openUpdateTournamentCompetitorModal, close: closeUpdateTournamentCompetitorModal }] = useDisclosure(false)
    const [isDeleteTournamentCompetitorModalOpen, { open: openDeleteTournamentCompetitorModal, close: closeDeleteTournamentCompetitorModal }] = useDisclosure(false)

    const createTournamentCompetitorForm = useForm<CreateTournamentCompetitorForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(createTournamentCompetitorFormSchema)
    })
    const updateTournamentCompetitorForm = useForm<UpdateTournamentCompetitorForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(updateTournamentCompetitorFormSchema)
    })
    const [tournamentCompetitorToBeDeleted, setTournamentCompetitorToBeDeleted] = useState<TournamentCompetitor | null>()
    const [tournamentCompetitor, setTournamentCompetitor] = useState<TournamentCompetitor>()

    const tournamentCompetitorList = usePaginatedList((page, pageSize) => tournamentCompetitorService.getAll(page, pageSize, { tournamentId: props.tournament.id }))
    const createTournamentCompetitorRequest = useRequest()
    const updateTournamentCompetitorRequest = useRequest()
    const deleteTournamentCompetitorRequest = useRequest()

    const tournamentCompetitorRows = tournamentCompetitorList.items.map(tc => (
        <Table.Tr key={tc.id}>
            <Table.Td>{tc.member.name}</Table.Td>
            <Table.Td>{tc.points}</Table.Td>
            <Table.Td>{convertPaymentMethodToView(tc.paymentMethod)}</Table.Td>
            <Table.Td>{convertBooleanToView(tc.isPaid)}</Table.Td>
            <Table.Td>
                <ActionButton label="Ver" icon={<EyeIcon/>} onClick={() => {
                    setTournamentCompetitor(tc)
                    openViewTournamentCompetitorModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Editar" icon={<PencilIcon/>} onClick={() => {
                    updateTournamentCompetitorForm.setValues({
                        id: tc.id,
                        paymentMethod: tc.paymentMethod,
                        isPaid: tc.isPaid
                    })
                    setTournamentCompetitor(tc)
                    openUpdateTournamentCompetitorModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setTournamentCompetitorToBeDeleted(tc)
                    openDeleteTournamentCompetitorModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    ))

    return (
        <>
            <Button variant="filled" onClick={() => {
                createTournamentCompetitorForm.reset()
                openCreateTournamentCompetitorModal()
            }}>Novo</Button>

            <ViewObjectModal
                isViewObjectModalOpen={isViewTournamentCompetitorModalOpen}
                closeViewObjectModal={closeViewTournamentCompetitorModal}
                labelValue={tournamentCompetitor ? convertTournamentCompetitorToView(tournamentCompetitor) : []}
            />

            <Modal
                opened={isCreateTournamentCompetitorModalOpen}
                onClose={() => {
                    closeCreateTournamentCompetitorModal()
                    createTournamentCompetitorRequest.clearError()
                }}
                title="Competidor"
                centered
            >
                <form onSubmit={createTournamentCompetitorForm.onSubmit((values) => createTournamentCompetitorRequest.run(async () => {
                    await tournamentCompetitorService.create({ ...values, tournamentId: props.tournament.id })

                    closeCreateTournamentCompetitorModal()
                    showSuccessNotification("Competidor salvo")
                    tournamentCompetitorList.reload()
                }))}>
                    <RequestErrorAlert error={createTournamentCompetitorRequest.error} onClose={createTournamentCompetitorRequest.clearError}/>
                    <SearchableCpfSelect
                        search={searchMemberOptionsByCpf}
                        key={createTournamentCompetitorForm.key('memberId')}
                        {...createTournamentCompetitorForm.getInputProps('memberId')}
                    />
                    <Select
                        withAsterisk
                        label="Forma de pagamento"
                        placeholder={convertPaymentMethodToView(PaymentMethod.PIX)}
                        data={paymentMethodOptions}
                        key={createTournamentCompetitorForm.key('paymentMethod')}
                        {...createTournamentCompetitorForm.getInputProps('paymentMethod')}
                    />
                    <Select
                        withAsterisk
                        label="Pago"
                        placeholder="Não"
                        data={booleanOptions}
                        key={createTournamentCompetitorForm.key('isPaid')}
                        {...createTournamentCompetitorForm.getInputProps('isPaid')}
                    />
                    <Button variant="filled" type="submit" loading={createTournamentCompetitorRequest.isLoading}>Salvar</Button>
                </form>
            </Modal>

            <Modal
                opened={isUpdateTournamentCompetitorModalOpen}
                onClose={() => {
                    closeUpdateTournamentCompetitorModal()
                    updateTournamentCompetitorRequest.clearError()
                }}
                title={`Competidor - ${tournamentCompetitor?.member.name ?? ""}`}
                centered
            >
                <form onSubmit={updateTournamentCompetitorForm.onSubmit((values) => updateTournamentCompetitorRequest.run(async () => {
                    await tournamentCompetitorService.updateById(values.id, values)

                    closeUpdateTournamentCompetitorModal()
                    showSuccessNotification("Competidor salvo")
                    tournamentCompetitorList.reload()
                }))}>
                    <RequestErrorAlert error={updateTournamentCompetitorRequest.error} onClose={updateTournamentCompetitorRequest.clearError}/>
                    <Select
                        withAsterisk
                        label="Forma de pagamento"
                        placeholder={convertPaymentMethodToView(PaymentMethod.PIX)}
                        data={paymentMethodOptions}
                        key={updateTournamentCompetitorForm.key('paymentMethod')}
                        {...updateTournamentCompetitorForm.getInputProps('paymentMethod')}
                    />
                    <Select
                        withAsterisk
                        label="Pago"
                        placeholder="Não"
                        data={booleanOptions}
                        key={updateTournamentCompetitorForm.key('isPaid')}
                        {...updateTournamentCompetitorForm.getInputProps('isPaid')}
                    />
                    <Button variant="filled" type="submit" loading={updateTournamentCompetitorRequest.isLoading}>Salvar</Button>
                </form>
            </Modal>

            <DeleteConfirmModal
                opened={isDeleteTournamentCompetitorModalOpen}
                onClose={closeDeleteTournamentCompetitorModal}
                title="Deletar competidor?"
                isLoading={deleteTournamentCompetitorRequest.isLoading}
                error={deleteTournamentCompetitorRequest.error}
                onCloseError={deleteTournamentCompetitorRequest.clearError}
                onConfirm={() => deleteTournamentCompetitorRequest.run(async () => {
                    if (!tournamentCompetitorToBeDeleted) {
                        throw new Error("tournamentCompetitorToBeDeleted is empty")
                    }

                    await tournamentCompetitorService.deleteById(tournamentCompetitorToBeDeleted.id)
                    setTournamentCompetitorToBeDeleted(null)
                    closeDeleteTournamentCompetitorModal()
                    showSuccessNotification("Competidor deletado")
                    tournamentCompetitorList.reload()
                })}
            >
                Tem certeza que deseja deletar a inscrição de {tournamentCompetitorToBeDeleted?.member.name} no torneio {props.tournament.name}?
            </DeleteConfirmModal>

            <RequestErrorAlert error={tournamentCompetitorList.error} onClose={tournamentCompetitorList.clearError}/>
            <RequestLoader isLoading={tournamentCompetitorList.isLoading} hasLoaded={tournamentCompetitorList.hasLoaded}>
                <Table striped mt="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Membro</Table.Th>
                            <Table.Th>Pontos</Table.Th>
                            <Table.Th>Forma de pagamento</Table.Th>
                            <Table.Th>Pago</Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>{tournamentCompetitorRows.length > 0 ? tournamentCompetitorRows : <EmptyTableRow colSpan={7}/>}</Table.Tbody>
                </Table>
            </RequestLoader>

            <ListPagination list={tournamentCompetitorList}/>
        </>
    )
}

interface CreateTournamentCompetitorForm {
    memberId: string
    paymentMethod: PaymentMethod
    isPaid: boolean
}

interface UpdateTournamentCompetitorForm {
    id: string
    paymentMethod: PaymentMethod
    isPaid: boolean
}
