import { Button, Group, Modal, NumberInput, Select, Stack, Table, Text, TextInput } from "@mantine/core";
import { DateInput, DateTimePicker } from "@mantine/dates";
import { useDisclosure } from "@mantine/hooks";
import { schemaResolver, useForm } from "@mantine/form";
import { useState } from "react";
import z from "zod/v4";
import { requiredText } from "../common/common.schema";
import { EyeIcon, PencilIcon, PlusIcon, TrashIcon, UsersIcon } from "@phosphor-icons/react";
import { gradingService, rankService } from "../instance";
import type { Grading } from "./grading.type";
import type { Rank } from "../rank/rank.type";
import { convertLocalDateTimeToForm, convertLocalDateTimeToService, convertLocalDateTimeToView } from "../common/common.converter";
import { convertGradingToView, convertRegistrationPeriodToView } from "./grading.converter";
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
import GradingCandidateModal from "../grading-candidate/GradingCandidateModal";

const gradingFormSchema = z.object({
    name: requiredText('Nome é obrigatório'),
    when: z.string({ error: 'Data e hora são obrigatórios' }),
    where: requiredText('Local é obrigatório'),
    registrationPeriodStart: z.string({ error: 'Início das inscrições é obrigatório' }),
    registrationPeriodEnd: z.string({ error: 'Fim das inscrições é obrigatório' }),
    prices: z.array(z.object({
        rankId: z.uuid({ error: 'Grau é obrigatório' }),
        price: z.number({ error: 'Preço é obrigatório' }).nonnegative({ error: 'Precisa ser não negativo' })
    }))
        .min(1, { error: 'Informe ao menos 1 preço' })
        .refine(prices => new Set(prices.map(p => p.rankId)).size === prices.length, { error: 'Há grau repetido nos preços' })
// The dates as yyyy-mm-dd, whose order is the one of the text. The registrations may open and close on the same day.
}).refine(grading => grading.registrationPeriodEnd >= grading.registrationPeriodStart, {
    error: 'O fim das inscrições não pode ser antes do início',
    path: ['registrationPeriodEnd']
})

const emptyPrice: GradingPriceForm = { rankId: null, price: '' }

const emptyGradingForm: GradingForm = {
    id: '',
    name: '',
    when: null,
    where: '',
    registrationPeriodStart: null,
    registrationPeriodEnd: null,
    prices: [emptyPrice]
}

export default function GradingTab() {
    const [isViewGradingModalOpen, { open: openViewGradingModal, close: closeViewGradingModal }] = useDisclosure(false)
    const [isCreateUpdateGradingModalOpen, { open: openCreateUpdateGradingModal, close: closeCreateUpdateGradingModal }] = useDisclosure(false)
    const [isDeleteGradingModalOpen, { open: openDeleteGradingModal, close: closeDeleteGradingModal }] = useDisclosure(false)
    const [isGradingCandidateModalOpen, { open: openGradingCandidateModal, close: closeGradingCandidateModal }] = useDisclosure(false)

    // Controlled, so the list of prices renders again when a price is added or removed
    const gradingForm = useForm<GradingForm>({
        initialValues: emptyGradingForm,
        validate: schemaResolver(gradingFormSchema)
    })
    const [gradingToBeDeleted, setGradingToBeDeleted] = useState<Grading | null>()
    const [grading, setGrading] = useState<Grading>()
    const [ranks, setRanks] = useState<Array<Rank>>([])

    const gradingList = usePaginatedList((page, pageSize) => gradingService.getAll(page, pageSize))
    const loadRankRequest = useLoadRequest()
    const saveGradingRequest = useRequest()
    const deleteGradingRequest = useRequest()

    // The ranks are the options of the prices
    function loadRankData() {
        loadRankRequest.run(
            () => rankService.getAllPages(),
            ranks => setRanks(ranks)
        )
    }

    useOnMount(() => loadRankData())

    const rankOptions = ranks.map(r => ({
        value: r.id,
        label: `${r.name} - ${r.category.name}`
    }))

    const gradingRows = gradingList.items.map(g => (
        <Table.Tr key={g.id}>
            <Table.Td>{g.name}</Table.Td>
            <Table.Td>{convertLocalDateTimeToView(g.when)}</Table.Td>
            <Table.Td>{g.where}</Table.Td>
            <Table.Td>{convertRegistrationPeriodToView(g)}</Table.Td>
            <Table.Td>
                <ActionButton label="Candidatos" icon={<UsersIcon/>} onClick={() => {
                    setGrading(g)
                    openGradingCandidateModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Ver" icon={<EyeIcon/>} onClick={() => {
                    setGrading(g)
                    openViewGradingModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Editar" icon={<PencilIcon/>} onClick={() => {
                    gradingForm.setValues({
                        id: g.id,
                        name: g.name,
                        when: convertLocalDateTimeToForm(g.when),
                        where: g.where,
                        registrationPeriodStart: g.registrationPeriodStart,
                        registrationPeriodEnd: g.registrationPeriodEnd,
                        prices: g.gradingPrices.map(gp => ({ rankId: gp.rank.id, price: gp.price }))
                    })
                    gradingForm.clearErrors()
                    openCreateUpdateGradingModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setGradingToBeDeleted(g)
                    openDeleteGradingModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    ))

    return (
        <>
            <Button variant="filled" onClick={() => {
                gradingForm.reset()
                openCreateUpdateGradingModal()
            }}>Novo</Button>

            <GradingCandidateModal
                opened={isGradingCandidateModalOpen}
                onClose={closeGradingCandidateModal}
                grading={grading}
            />

            <ViewObjectModal
                isViewObjectModalOpen={isViewGradingModalOpen}
                closeViewObjectModal={closeViewGradingModal}
                labelValue={grading ? convertGradingToView(grading) : []}
            />

            <Modal
                opened={isCreateUpdateGradingModalOpen}
                onClose={() => {
                    closeCreateUpdateGradingModal()
                    saveGradingRequest.clearError()
                }}
                title="Graduação"
                size="lg"
                centered
            >
                <form onSubmit={gradingForm.onSubmit((values) => saveGradingRequest.run(async () => {
                    // The schema checked the fields, so none of them is empty here
                    const input = {
                        name: values.name,
                        when: convertLocalDateTimeToService(values.when as string),
                        where: values.where,
                        registrationPeriodStart: values.registrationPeriodStart as string,
                        registrationPeriodEnd: values.registrationPeriodEnd as string,
                        prices: Object.fromEntries(values.prices.map(p => [p.rankId as string, p.price as number]))
                    }
                    if (!values.id) {
                        await gradingService.create(input)
                    } else {
                        await gradingService.updateById(values.id, input)
                    }

                    closeCreateUpdateGradingModal()
                    showSuccessNotification("Graduação salva")
                    gradingList.reload()
                }))}>
                    <RequestErrorAlert error={loadRankRequest.error} onClose={loadRankRequest.clearError}/>
                    <RequestErrorAlert error={saveGradingRequest.error} onClose={saveGradingRequest.clearError}/>
                    <TextInput
                        withAsterisk
                        label="Nome"
                        placeholder="Graduação - 2º semestre"
                        {...gradingForm.getInputProps('name')}
                    />
                    <DateTimePicker
                        withAsterisk
                        label="Data e hora"
                        placeholder="dd/mm/aaaa hh:mm"
                        valueFormat="DD/MM/YYYY HH:mm"
                        {...gradingForm.getInputProps('when')}
                    />
                    <TextInput
                        withAsterisk
                        label="Local"
                        placeholder="Sede"
                        {...gradingForm.getInputProps('where')}
                    />
                    <Group grow>
                        <DateInput
                            withAsterisk
                            label="Início das inscrições"
                            placeholder="dd/mm/aaaa"
                            valueFormat="DD/MM/YYYY"
                            {...gradingForm.getInputProps('registrationPeriodStart')}
                        />
                        <DateInput
                            withAsterisk
                            label="Fim das inscrições"
                            placeholder="dd/mm/aaaa"
                            valueFormat="DD/MM/YYYY"
                            {...gradingForm.getInputProps('registrationPeriodEnd')}
                        />
                    </Group>

                    <Text fw={500} size="sm" mt="md">Preços por grau pretendido</Text>
                    <Stack gap="xs">
                        {gradingForm.values.prices.map((_price, index) => (
                            <Group key={index} align="flex-end">
                                <Select
                                    flex={1}
                                    withAsterisk
                                    searchable
                                    label="Grau"
                                    placeholder="Faixa Azul - Adulto"
                                    data={rankOptions}
                                    {...gradingForm.getInputProps(`prices.${index}.rankId`)}
                                />
                                <NumberInput
                                    flex={1}
                                    withAsterisk
                                    label="Preço"
                                    placeholder="R$ 100,00"
                                    prefix="R$ "
                                    decimalSeparator=","
                                    thousandSeparator="."
                                    decimalScale={2}
                                    fixedDecimalScale
                                    {...gradingForm.getInputProps(`prices.${index}.price`)}
                                />
                                <ActionButton
                                    label="Remover preço"
                                    icon={<TrashIcon/>}
                                    onClick={() => gradingForm.removeListItem('prices', index)}
                                />
                            </Group>
                        ))}
                    </Stack>
                    {typeof gradingForm.errors.prices === 'string' && (
                        <Text c="red" size="sm">{gradingForm.errors.prices}</Text>
                    )}
                    <Button
                        variant="light"
                        leftSection={<PlusIcon/>}
                        mt="xs"
                        onClick={() => gradingForm.insertListItem('prices', emptyPrice)}
                    >Adicionar preço</Button>

                    <Group justify="flex-end" mt="md">
                        <Button variant="filled" type="submit" loading={saveGradingRequest.isLoading}>Salvar</Button>
                    </Group>
                </form>
            </Modal>

            <DeleteConfirmModal
                opened={isDeleteGradingModalOpen}
                onClose={closeDeleteGradingModal}
                title="Deletar graduação?"
                isLoading={deleteGradingRequest.isLoading}
                error={deleteGradingRequest.error}
                onCloseError={deleteGradingRequest.clearError}
                onConfirm={() => deleteGradingRequest.run(async () => {
                    if (!gradingToBeDeleted) {
                        throw new Error("gradingToBeDeleted is empty")
                    }

                    await gradingService.deleteById(gradingToBeDeleted.id)
                    setGradingToBeDeleted(null)
                    closeDeleteGradingModal()
                    showSuccessNotification("Graduação deletada")
                    gradingList.reload()
                })}
            >
                Tem certeza que deseja deletar a graduação {gradingToBeDeleted?.name}?
            </DeleteConfirmModal>

            <RequestErrorAlert error={gradingList.error} onClose={gradingList.clearError}/>
            <RequestLoader isLoading={gradingList.isLoading} hasLoaded={gradingList.hasLoaded}>
                <Table striped mt="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Nome</Table.Th>
                            <Table.Th>Data</Table.Th>
                            <Table.Th>Local</Table.Th>
                            <Table.Th>Inscrições</Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>{gradingRows.length > 0 ? gradingRows : <EmptyTableRow colSpan={8}/>}</Table.Tbody>
                </Table>
            </RequestLoader>

            <ListPagination list={gradingList}/>
        </>
    )
}

// The fields start empty, so the inputs of the controlled form start without a value
interface GradingForm {
    id: string
    name: string
    when: string | null
    where: string
    registrationPeriodStart: string | null
    registrationPeriodEnd: string | null
    prices: Array<GradingPriceForm>
}

interface GradingPriceForm {
    rankId: string | null
    price: number | string
}
