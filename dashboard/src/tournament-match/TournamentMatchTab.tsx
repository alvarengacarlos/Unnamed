import { Button, Group, Modal, Select, Table, TextInput } from "@mantine/core";
import { DateTimePicker } from "@mantine/dates";
import { useDisclosure } from "@mantine/hooks";
import { schemaResolver, useForm } from "@mantine/form";
import { useState } from "react";
import z from "zod/v4";
import { requiredText } from "../common/common.schema";
import { EyeIcon, PencilIcon, TrashIcon } from "@phosphor-icons/react";
import { tournamentCompetitorService, tournamentMatchService } from "../instance";
import { TournamentMatchResult, type TournamentMatch } from "./tournament-match.type";
import type { Tournament } from "../tournament/tournament.type";
import type { TournamentCompetitor } from "../tournament-competitor/tournament-competitor.type";
import { convertLocalDateTimeToForm, convertLocalDateTimeToService, convertLocalDateTimeToView } from "../common/common.converter";
import { convertTournamentMatchResultOfMatchToView, convertTournamentMatchResultToView, convertTournamentMatchToView } from "./tournament-match.converter";
import { useLoadRequest, useOnMount, useRequest } from "../common/hook/useRequest.hook";
import { usePaginatedList } from "../common/hook/usePaginatedList.hook";
import { showSuccessNotification } from "../common/common.notification";
import ViewObjectModal from "../common/component/ViewObjectModal.component";
import RequestErrorAlert from "../common/component/RequestErrorAlert.component";
import RequestLoader from "../common/component/RequestLoader.component";
import ActionButton from "../common/component/ActionButton.component";
import DeleteConfirmModal from "../common/component/DeleteConfirmModal.component";
import EmptyTableRow from "../common/component/EmptyTableRow.component";
import ListPagination from "../common/component/ListPagination.component";
import TournamentMatchBulkModal from "./TournamentMatchBulkModal";

const whenWhereSchema = z.object({
    when: z.string({ error: 'Data e hora são obrigatórios' }),
    where: requiredText('Local é obrigatório')
})

// The competitors are chosen only at the creation
const createTournamentMatchFormSchema = whenWhereSchema.extend({
    tournamentCompetitorAId: z.uuid({ error: 'Competidor A é obrigatório' }),
    tournamentCompetitorBId: z.uuid({ error: 'Competidor B é obrigatório' })
}).refine(match => match.tournamentCompetitorAId !== match.tournamentCompetitorBId, {
    error: 'Os competidores precisam ser diferentes',
    path: ['tournamentCompetitorBId']
})

// The result is set only after the match is created
const updateTournamentMatchFormSchema = whenWhereSchema.extend({
    result: z.enum(TournamentMatchResult, { error: 'Resultado é obrigatório' })
})

const resultOptions = Object.values(TournamentMatchResult).map(result => ({
    value: result,
    label: convertTournamentMatchResultToView(result)
}))

interface Props {
    // The tournament whose matches are managed
    tournament: Tournament
}

export default function TournamentMatchTab(props: Props) {
    const [isViewTournamentMatchModalOpen, { open: openViewTournamentMatchModal, close: closeViewTournamentMatchModal }] = useDisclosure(false)
    const [isCreateTournamentMatchModalOpen, { open: openCreateTournamentMatchModal, close: closeCreateTournamentMatchModal }] = useDisclosure(false)
    const [isUpdateTournamentMatchModalOpen, { open: openUpdateTournamentMatchModal, close: closeUpdateTournamentMatchModal }] = useDisclosure(false)
    const [isDeleteTournamentMatchModalOpen, { open: openDeleteTournamentMatchModal, close: closeDeleteTournamentMatchModal }] = useDisclosure(false)
    const [isTournamentMatchBulkModalOpen, { open: openTournamentMatchBulkModal, close: closeTournamentMatchBulkModal }] = useDisclosure(false)

    const createTournamentMatchForm = useForm<CreateTournamentMatchForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(createTournamentMatchFormSchema)
    })
    const updateTournamentMatchForm = useForm<UpdateTournamentMatchForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(updateTournamentMatchFormSchema)
    })
    const [tournamentMatchToBeDeleted, setTournamentMatchToBeDeleted] = useState<TournamentMatch | null>()
    const [tournamentMatch, setTournamentMatch] = useState<TournamentMatch>()
    const [tournamentCompetitors, setTournamentCompetitors] = useState<Array<TournamentCompetitor>>([])

    const tournamentMatchList = usePaginatedList((page, pageSize) => tournamentMatchService.getAll(page, pageSize, { tournamentId: props.tournament.id }))
    const loadFormOptionsRequest = useLoadRequest()
    const createTournamentMatchRequest = useRequest()
    const updateTournamentMatchRequest = useRequest()
    const deleteTournamentMatchRequest = useRequest()

    // The competitors of the tournament are the options of the forms
    function loadFormOptionsData() {
        loadFormOptionsRequest.run(
            () => tournamentCompetitorService.getAllPages({ tournamentId: props.tournament.id }),
            tournamentCompetitors => setTournamentCompetitors(tournamentCompetitors)
        )
    }

    useOnMount(() => loadFormOptionsData())

    const tournamentCompetitorOptions = tournamentCompetitors.map(tc => ({
        value: tc.id,
        label: tc.member.name
    }))

    const tournamentMatchRows = tournamentMatchList.items.map(tm => (
        <Table.Tr key={tm.id}>
            <Table.Td>{tm.competitorA.member.name}</Table.Td>
            <Table.Td>{tm.competitorB.member.name}</Table.Td>
            <Table.Td>{convertLocalDateTimeToView(tm.when)}</Table.Td>
            <Table.Td>{convertTournamentMatchResultOfMatchToView(tm)}</Table.Td>
            <Table.Td>
                <ActionButton label="Ver" icon={<EyeIcon/>} onClick={() => {
                    setTournamentMatch(tm)
                    openViewTournamentMatchModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Editar" icon={<PencilIcon/>} onClick={() => {
                    updateTournamentMatchForm.setValues({
                        id: tm.id,
                        when: convertLocalDateTimeToForm(tm.when),
                        where: tm.where,
                        result: tm.result
                    })
                    setTournamentMatch(tm)
                    openUpdateTournamentMatchModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setTournamentMatchToBeDeleted(tm)
                    openDeleteTournamentMatchModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    ))

    return (
        <>
            <Group gap="xs">
                <Button variant="filled" onClick={() => {
                    createTournamentMatchForm.reset()
                    openCreateTournamentMatchModal()
                }}>Novo</Button>
                <Button variant="light" onClick={openTournamentMatchBulkModal}>Novo em lote</Button>
            </Group>

            <TournamentMatchBulkModal
                opened={isTournamentMatchBulkModalOpen}
                onClose={closeTournamentMatchBulkModal}
                onCreated={() => tournamentMatchList.reload()}
                tournament={props.tournament}
                tournamentCompetitors={tournamentCompetitors}
            />

            <ViewObjectModal
                isViewObjectModalOpen={isViewTournamentMatchModalOpen}
                closeViewObjectModal={closeViewTournamentMatchModal}
                labelValue={tournamentMatch ? convertTournamentMatchToView(tournamentMatch) : []}
            />

            <Modal
                opened={isCreateTournamentMatchModalOpen}
                onClose={() => {
                    closeCreateTournamentMatchModal()
                    createTournamentMatchRequest.clearError()
                }}
                title="Luta"
                centered
            >
                <form onSubmit={createTournamentMatchForm.onSubmit((values) => createTournamentMatchRequest.run(async () => {
                    await tournamentMatchService.create({
                        ...values,
                        tournamentId: props.tournament.id,
                        when: convertLocalDateTimeToService(values.when)
                    })

                    closeCreateTournamentMatchModal()
                    showSuccessNotification("Luta salva")
                    tournamentMatchList.reload()
                }))}>
                    <RequestErrorAlert error={loadFormOptionsRequest.error} onClose={loadFormOptionsRequest.clearError}/>
                    <RequestErrorAlert error={createTournamentMatchRequest.error} onClose={createTournamentMatchRequest.clearError}/>
                    <Select
                        withAsterisk
                        searchable
                        label="Competidor A"
                        placeholder="João Silva"
                        data={tournamentCompetitorOptions}
                        key={createTournamentMatchForm.key('tournamentCompetitorAId')}
                        {...createTournamentMatchForm.getInputProps('tournamentCompetitorAId')}
                    />
                    <Select
                        withAsterisk
                        searchable
                        label="Competidor B"
                        placeholder="Pedro Souza"
                        data={tournamentCompetitorOptions}
                        key={createTournamentMatchForm.key('tournamentCompetitorBId')}
                        {...createTournamentMatchForm.getInputProps('tournamentCompetitorBId')}
                    />
                    <DateTimePicker
                        withAsterisk
                        label="Data e hora"
                        placeholder="dd/mm/aaaa hh:mm"
                        valueFormat="DD/MM/YYYY HH:mm"
                        key={createTournamentMatchForm.key('when')}
                        {...createTournamentMatchForm.getInputProps('when')}
                    />
                    <TextInput
                        withAsterisk
                        label="Local"
                        placeholder="Tatame 1"
                        key={createTournamentMatchForm.key('where')}
                        {...createTournamentMatchForm.getInputProps('where')}
                    />
                    <Button variant="filled" type="submit" loading={createTournamentMatchRequest.isLoading}>Salvar</Button>
                </form>
            </Modal>

            <Modal
                opened={isUpdateTournamentMatchModalOpen}
                onClose={() => {
                    closeUpdateTournamentMatchModal()
                    updateTournamentMatchRequest.clearError()
                }}
                title={tournamentMatch ? `${tournamentMatch.competitorA.member.name} x ${tournamentMatch.competitorB.member.name}` : "Luta"}
                centered
            >
                <form onSubmit={updateTournamentMatchForm.onSubmit((values) => updateTournamentMatchRequest.run(async () => {
                    await tournamentMatchService.updateById(values.id, { ...values, when: convertLocalDateTimeToService(values.when) })

                    closeUpdateTournamentMatchModal()
                    showSuccessNotification("Luta salva")
                    tournamentMatchList.reload()
                }))}>
                    <RequestErrorAlert error={updateTournamentMatchRequest.error} onClose={updateTournamentMatchRequest.clearError}/>
                    <DateTimePicker
                        withAsterisk
                        label="Data e hora"
                        placeholder="dd/mm/aaaa hh:mm"
                        valueFormat="DD/MM/YYYY HH:mm"
                        key={updateTournamentMatchForm.key('when')}
                        {...updateTournamentMatchForm.getInputProps('when')}
                    />
                    <TextInput
                        withAsterisk
                        label="Local"
                        placeholder="Tatame 1"
                        key={updateTournamentMatchForm.key('where')}
                        {...updateTournamentMatchForm.getInputProps('where')}
                    />
                    <Select
                        withAsterisk
                        label="Resultado"
                        placeholder={convertTournamentMatchResultToView(TournamentMatchResult.PENDING)}
                        // With the names of the competitors, so A and B are not confused
                        data={resultOptions.map(option => ({
                            ...option,
                            label: option.value === TournamentMatchResult.A && tournamentMatch
                                ? `Vitória de ${tournamentMatch.competitorA.member.name}`
                                : option.value === TournamentMatchResult.B && tournamentMatch
                                    ? `Vitória de ${tournamentMatch.competitorB.member.name}`
                                    : option.label
                        }))}
                        key={updateTournamentMatchForm.key('result')}
                        {...updateTournamentMatchForm.getInputProps('result')}
                    />
                    <Button variant="filled" type="submit" loading={updateTournamentMatchRequest.isLoading}>Salvar</Button>
                </form>
            </Modal>

            <DeleteConfirmModal
                opened={isDeleteTournamentMatchModalOpen}
                onClose={closeDeleteTournamentMatchModal}
                title="Deletar luta?"
                isLoading={deleteTournamentMatchRequest.isLoading}
                error={deleteTournamentMatchRequest.error}
                onCloseError={deleteTournamentMatchRequest.clearError}
                onConfirm={() => deleteTournamentMatchRequest.run(async () => {
                    if (!tournamentMatchToBeDeleted) {
                        throw new Error("tournamentMatchToBeDeleted is empty")
                    }

                    await tournamentMatchService.deleteById(tournamentMatchToBeDeleted.id)
                    setTournamentMatchToBeDeleted(null)
                    closeDeleteTournamentMatchModal()
                    showSuccessNotification("Luta deletada")
                    tournamentMatchList.reload()
                })}
            >
                Tem certeza que deseja deletar a luta entre {tournamentMatchToBeDeleted?.competitorA.member.name} e {tournamentMatchToBeDeleted?.competitorB.member.name}?
            </DeleteConfirmModal>

            <RequestErrorAlert error={tournamentMatchList.error} onClose={tournamentMatchList.clearError}/>
            <RequestLoader isLoading={tournamentMatchList.isLoading} hasLoaded={tournamentMatchList.hasLoaded}>
                <Table striped mt="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Competidor A</Table.Th>
                            <Table.Th>Competidor B</Table.Th>
                            <Table.Th>Data</Table.Th>
                            <Table.Th>Resultado</Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>{tournamentMatchRows.length > 0 ? tournamentMatchRows : <EmptyTableRow colSpan={7}/>}</Table.Tbody>
                </Table>
            </RequestLoader>

            <ListPagination list={tournamentMatchList}/>
        </>
    )
}

interface CreateTournamentMatchForm {
    tournamentCompetitorAId: string
    tournamentCompetitorBId: string
    when: string
    where: string
}

interface UpdateTournamentMatchForm {
    id: string
    when: string
    where: string
    result: TournamentMatchResult
}
