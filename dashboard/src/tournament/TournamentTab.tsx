import { Button, Fieldset, Group, Modal, NumberInput, Select, Table, TextInput } from "@mantine/core";
import { DateTimePicker } from "@mantine/dates";
import { useDisclosure } from "@mantine/hooks";
import { schemaResolver, useForm } from "@mantine/form";
import { useState } from "react";
import z from "zod/v4";
import { requiredText } from "../common/common.schema";
import { EyeIcon, PencilIcon, SwordIcon, TrashIcon, UsersThreeIcon } from "@phosphor-icons/react";
import { rankService, tournamentService } from "../instance";
import { TournamentFormat, type Tournament } from "./tournament.type";
import { Gender } from "../member/member.type";
import type { Rank } from "../rank/rank.type";
import { booleanOptions, convertLocalDateTimeToForm, convertLocalDateTimeToService, convertLocalDateTimeToView, convertPriceToView } from "../common/common.converter";
import { convertGenderToView } from "../member/member.converter";
import { convertTournamentFormatToView, convertTournamentToView } from "./tournament.converter";
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
import PossibleCompetitorsModal from "./PossibleCompetitorsModal";
import TournamentCompetitionModal from "./TournamentCompetitionModal";

// A criterion left empty does not divide the competitors: an empty NumberInput gives '' and a cleared Select, null
function optionalPositiveNumber(error: string) {
    return z.number({ error }).positive({ error }).or(z.literal('')).optional()
}

const tournamentFormSchema = z.object({
    name: requiredText('Nome é obrigatório'),
    format: z.enum(TournamentFormat, { error: 'Formato é obrigatório' }),
    participationPrice: z.number({ error: 'Preço de participação é obrigatório' }).nonnegative({ error: 'Precisa ser não negativo' }),
    startDateTime: z.string({ error: 'Início é obrigatório' }),
    endDateTime: z.string({ error: 'Fim é obrigatório' }),
    where: requiredText('Local é obrigatório'),
    gender: z.enum(Gender).nullable().optional(),
    weight: optionalPositiveNumber('Peso deve ser positivo'),
    height: optionalPositiveNumber('Altura deve ser positiva'),
    age: optionalPositiveNumber('Idade deve ser positiva'),
    isPwd: z.boolean().nullable().optional(),
    rankId: z.uuid().nullable().optional()
// The dates and times as yyyy-mm-dd hh:mm:ss, whose order is the one of the text
}).refine(tournament => tournament.endDateTime >= tournament.startDateTime, {
    error: 'O fim não pode ser antes do início',
    path: ['endDateTime']
})

const formatOptions = Object.values(TournamentFormat).map(format => ({
    value: format,
    label: convertTournamentFormatToView(format)
}))

const genderOptions = [Gender.MALE, Gender.FEMALE].map(gender => ({
    value: gender,
    label: convertGenderToView(gender)
}))

// An empty field of the form is a criterion the API takes as null
function convertEmptyToNull<T>(value: T | '' | null | undefined): T | null {
    return value === '' || value === undefined ? null : value
}

export default function TournamentTab() {
    const [isViewTournamentModalOpen, { open: openViewTournamentModal, close: closeViewTournamentModal }] = useDisclosure(false)
    const [isCreateUpdateTournamentModalOpen, { open: openCreateUpdateTournamentModal, close: closeCreateUpdateTournamentModal }] = useDisclosure(false)
    const [isDeleteTournamentModalOpen, { open: openDeleteTournamentModal, close: closeDeleteTournamentModal }] = useDisclosure(false)
    const [isPossibleCompetitorsModalOpen, { open: openPossibleCompetitorsModal, close: closePossibleCompetitorsModal }] = useDisclosure(false)
    const [isTournamentCompetitionModalOpen, { open: openTournamentCompetitionModal, close: closeTournamentCompetitionModal }] = useDisclosure(false)

    const tournamentForm = useForm<TournamentForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(tournamentFormSchema)
    })
    const [tournamentToBeDeleted, setTournamentToBeDeleted] = useState<Tournament | null>()
    const [tournament, setTournament] = useState<Tournament>()
    const [ranks, setRanks] = useState<Array<Rank>>([])

    const tournamentList = usePaginatedList((page, pageSize) => tournamentService.getAll(page, pageSize))
    const loadRankRequest = useLoadRequest()
    const saveTournamentRequest = useRequest()
    const deleteTournamentRequest = useRequest()

    // The ranks are the options of the rank criterion
    function loadRankData() {
        loadRankRequest.run(
            () => rankService.getAllPages(),
            ranks => setRanks(ranks)
        )
    }

    useOnMount(() => loadRankData())

    const tournamentRows = tournamentList.items.map(t => (
        <Table.Tr key={t.id}>
            <Table.Td>{t.name}</Table.Td>
            <Table.Td>{convertTournamentFormatToView(t.format)}</Table.Td>
            <Table.Td>{convertLocalDateTimeToView(t.startDateTime)}</Table.Td>
            <Table.Td>{t.where}</Table.Td>
            <Table.Td>{convertPriceToView(t.participationPrice)}</Table.Td>
            <Table.Td>
                <ActionButton label="Competidores e lutas" icon={<SwordIcon/>} onClick={() => {
                    setTournament(t)
                    openTournamentCompetitionModal()
                }}/>
            </Table.Td>
            <Table.Td>
                {/* The API does not search competitors for a title match */}
                {t.format !== TournamentFormat.TITLE_MATCH && (
                    <ActionButton label="Possíveis competidores" icon={<UsersThreeIcon/>} onClick={() => {
                        setTournament(t)
                        openPossibleCompetitorsModal()
                    }}/>
                )}
            </Table.Td>
            <Table.Td>
                <ActionButton label="Ver" icon={<EyeIcon/>} onClick={() => {
                    setTournament(t)
                    openViewTournamentModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Editar" icon={<PencilIcon/>} onClick={() => {
                    tournamentForm.setValues({
                        id: t.id,
                        name: t.name,
                        format: t.format,
                        participationPrice: t.participationPrice,
                        startDateTime: convertLocalDateTimeToForm(t.startDateTime),
                        endDateTime: convertLocalDateTimeToForm(t.endDateTime),
                        where: t.where,
                        gender: t.divisionCriterion.gender ?? null,
                        weight: t.divisionCriterion.weight ?? '',
                        height: t.divisionCriterion.height ?? '',
                        age: t.divisionCriterion.age ?? '',
                        isPwd: t.divisionCriterion.isPwd ?? null,
                        rankId: t.divisionCriterion.rank?.id ?? null
                    })
                    openCreateUpdateTournamentModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setTournamentToBeDeleted(t)
                    openDeleteTournamentModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    ))

    return (
        <>
            <Button variant="filled" onClick={() => {
                tournamentForm.reset()
                openCreateUpdateTournamentModal()
            }}>Novo</Button>

            <TournamentCompetitionModal
                opened={isTournamentCompetitionModalOpen}
                onClose={closeTournamentCompetitionModal}
                tournament={tournament}
            />

            <PossibleCompetitorsModal
                opened={isPossibleCompetitorsModalOpen}
                onClose={closePossibleCompetitorsModal}
                tournament={tournament}
            />

            <ViewObjectModal
                isViewObjectModalOpen={isViewTournamentModalOpen}
                closeViewObjectModal={closeViewTournamentModal}
                labelValue={tournament ? convertTournamentToView(tournament) : []}
            />

            <Modal
                opened={isCreateUpdateTournamentModalOpen}
                onClose={() => {
                    closeCreateUpdateTournamentModal()
                    saveTournamentRequest.clearError()
                }}
                title="Torneio"
                size="lg"
                centered
            >
                <form onSubmit={tournamentForm.onSubmit((values) => saveTournamentRequest.run(async () => {
                    const input = {
                        name: values.name,
                        format: values.format,
                        participationPrice: values.participationPrice,
                        startDateTime: convertLocalDateTimeToService(values.startDateTime),
                        endDateTime: convertLocalDateTimeToService(values.endDateTime),
                        where: values.where,
                        divisionCriterion: {
                            GENDER: convertEmptyToNull(values.gender),
                            WEIGHT: convertEmptyToNull(values.weight),
                            HEIGHT: convertEmptyToNull(values.height),
                            AGE: convertEmptyToNull(values.age),
                            IS_PWD: convertEmptyToNull(values.isPwd),
                            RANK: convertEmptyToNull(values.rankId)
                        }
                    }
                    if (!values.id) {
                        await tournamentService.create(input)
                    } else {
                        await tournamentService.updateById(values.id, input)
                    }

                    closeCreateUpdateTournamentModal()
                    showSuccessNotification("Torneio salvo")
                    tournamentList.reload()
                }))}>
                    <RequestErrorAlert error={loadRankRequest.error} onClose={loadRankRequest.clearError}/>
                    <RequestErrorAlert error={saveTournamentRequest.error} onClose={saveTournamentRequest.clearError}/>
                    <TextInput
                        withAsterisk
                        label="Nome"
                        placeholder="Copa de Inverno"
                        key={tournamentForm.key('name')}
                        {...tournamentForm.getInputProps('name')}
                    />
                    <Select
                        withAsterisk
                        label="Formato"
                        placeholder={convertTournamentFormatToView(TournamentFormat.SINGLE_ELIMINATION)}
                        data={formatOptions}
                        key={tournamentForm.key('format')}
                        {...tournamentForm.getInputProps('format')}
                    />
                    <NumberInput
                        withAsterisk
                        label="Preço de participação"
                        placeholder="R$ 50,00"
                        prefix="R$ "
                        decimalSeparator=","
                        thousandSeparator="."
                        decimalScale={2}
                        fixedDecimalScale
                        key={tournamentForm.key('participationPrice')}
                        {...tournamentForm.getInputProps('participationPrice')}
                    />
                    <Group grow>
                        <DateTimePicker
                            withAsterisk
                            label="Início"
                            placeholder="dd/mm/aaaa hh:mm"
                            valueFormat="DD/MM/YYYY HH:mm"
                            key={tournamentForm.key('startDateTime')}
                            {...tournamentForm.getInputProps('startDateTime')}
                        />
                        <DateTimePicker
                            withAsterisk
                            label="Fim"
                            placeholder="dd/mm/aaaa hh:mm"
                            valueFormat="DD/MM/YYYY HH:mm"
                            key={tournamentForm.key('endDateTime')}
                            {...tournamentForm.getInputProps('endDateTime')}
                        />
                    </Group>
                    <TextInput
                        withAsterisk
                        label="Local"
                        placeholder="Ginásio Municipal"
                        key={tournamentForm.key('where')}
                        {...tournamentForm.getInputProps('where')}
                    />

                    <Fieldset legend="Critérios de divisão (os vazios não dividem)" mt="md">
                        <Group grow>
                            <Select
                                clearable
                                label="Sexo"
                                placeholder="Qualquer"
                                data={genderOptions}
                                key={tournamentForm.key('gender')}
                                {...tournamentForm.getInputProps('gender')}
                            />
                            <Select
                                clearable
                                label="PCD"
                                placeholder="Qualquer"
                                data={booleanOptions}
                                key={tournamentForm.key('isPwd')}
                                {...tournamentForm.getInputProps('isPwd')}
                            />
                        </Group>
                        <Group grow>
                            <NumberInput
                                label="Peso"
                                placeholder="Qualquer"
                                suffix=" kg"
                                decimalSeparator=","
                                decimalScale={2}
                                key={tournamentForm.key('weight')}
                                {...tournamentForm.getInputProps('weight')}
                            />
                            <NumberInput
                                label="Altura"
                                placeholder="Qualquer"
                                suffix=" m"
                                decimalSeparator=","
                                decimalScale={2}
                                key={tournamentForm.key('height')}
                                {...tournamentForm.getInputProps('height')}
                            />
                            <NumberInput
                                label="Idade"
                                placeholder="Qualquer"
                                suffix=" anos"
                                allowDecimal={false}
                                key={tournamentForm.key('age')}
                                {...tournamentForm.getInputProps('age')}
                            />
                        </Group>
                        <Select
                            clearable
                            searchable
                            label="Grau"
                            placeholder="Qualquer"
                            data={ranks.map(r => ({
                                value: r.id,
                                label: `${r.name} - ${r.category.name}`
                            }))}
                            key={tournamentForm.key('rankId')}
                            {...tournamentForm.getInputProps('rankId')}
                        />
                    </Fieldset>

                    <Group justify="flex-end" mt="md">
                        <Button variant="filled" type="submit" loading={saveTournamentRequest.isLoading}>Salvar</Button>
                    </Group>
                </form>
            </Modal>

            <DeleteConfirmModal
                opened={isDeleteTournamentModalOpen}
                onClose={closeDeleteTournamentModal}
                title="Deletar torneio?"
                isLoading={deleteTournamentRequest.isLoading}
                error={deleteTournamentRequest.error}
                onCloseError={deleteTournamentRequest.clearError}
                onConfirm={() => deleteTournamentRequest.run(async () => {
                    if (!tournamentToBeDeleted) {
                        throw new Error("tournamentToBeDeleted is empty")
                    }

                    await tournamentService.deleteById(tournamentToBeDeleted.id)
                    setTournamentToBeDeleted(null)
                    closeDeleteTournamentModal()
                    showSuccessNotification("Torneio deletado")
                    tournamentList.reload()
                })}
            >
                Tem certeza que deseja deletar o torneio {tournamentToBeDeleted?.name}?
            </DeleteConfirmModal>

            <RequestErrorAlert error={tournamentList.error} onClose={tournamentList.clearError}/>
            <RequestLoader isLoading={tournamentList.isLoading} hasLoaded={tournamentList.hasLoaded}>
                <Table striped mt="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Nome</Table.Th>
                            <Table.Th>Formato</Table.Th>
                            <Table.Th>Início</Table.Th>
                            <Table.Th>Local</Table.Th>
                            <Table.Th>Preço</Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>{tournamentRows.length > 0 ? tournamentRows : <EmptyTableRow colSpan={10}/>}</Table.Tbody>
                </Table>
            </RequestLoader>

            <ListPagination list={tournamentList}/>
        </>
    )
}

interface TournamentForm {
    id: string
    name: string
    format: TournamentFormat
    participationPrice: number
    startDateTime: string
    endDateTime: string
    where: string
    gender: Gender | null
    weight: number | ''
    height: number | ''
    age: number | ''
    isPwd: boolean | null
    rankId: string | null
}
