import { Button, Modal, Select, Table } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { schemaResolver, useForm } from "@mantine/form";
import { useState } from "react";
import z from "zod/v4";
import { EyeIcon, PencilIcon, TrashIcon } from "@phosphor-icons/react";
import { gradingCandidateService, rankService } from "../instance";
import type { GradingCandidate } from "./grading-candidate.type";
import type { Grading } from "../grading/grading.type";
import type { Rank } from "../rank/rank.type";
import { PaymentMethod } from "../common/common.type";
import { booleanOptions, convertBooleanToView, convertPaymentMethodToView, paymentMethodOptions } from "../common/common.converter";
import { convertGradingCandidateToView } from "./grading-candidate.converter";
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
import SearchableCpfSelect from "../common/component/SearchableCpfSelect.component";
import { searchMemberOptionsByCpf } from "../member/member.search";

const updateGradingCandidateFormSchema = z.object({
    rankId: z.uuid({ error: 'Grau pretendido é obrigatório' }),
    paymentMethod: z.enum(PaymentMethod, { error: 'Forma de pagamento é obrigatório' }),
    isPaid: z.boolean({ error: 'Pago é obrigatório' })
})

// The member is chosen only at the creation
const createGradingCandidateFormSchema = updateGradingCandidateFormSchema.extend({
    memberId: z.uuid({ error: 'Membro é obrigatório' })
})

interface Props {
    opened: boolean
    onClose: () => void
    // The grading whose candidates are managed
    grading?: Grading
}

export default function GradingCandidateModal(props: Props) {
    return (
        <Modal opened={props.opened} onClose={props.onClose} title={`Candidatos - ${props.grading?.name ?? ""}`} fullScreen>
            {/* The content mounts each time the modal opens, so it starts clean for the grading chosen */}
            {props.grading && <GradingCandidateContent grading={props.grading}/>}
        </Modal>
    )
}

interface ContentProps {
    grading: Grading
}

function GradingCandidateContent(props: ContentProps) {
    const [isViewGradingCandidateModalOpen, { open: openViewGradingCandidateModal, close: closeViewGradingCandidateModal }] = useDisclosure(false)
    const [isCreateGradingCandidateModalOpen, { open: openCreateGradingCandidateModal, close: closeCreateGradingCandidateModal }] = useDisclosure(false)
    const [isUpdateGradingCandidateModalOpen, { open: openUpdateGradingCandidateModal, close: closeUpdateGradingCandidateModal }] = useDisclosure(false)
    const [isDeleteGradingCandidateModalOpen, { open: openDeleteGradingCandidateModal, close: closeDeleteGradingCandidateModal }] = useDisclosure(false)

    const createGradingCandidateForm = useForm<CreateGradingCandidateForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(createGradingCandidateFormSchema)
    })
    const updateGradingCandidateForm = useForm<UpdateGradingCandidateForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(updateGradingCandidateFormSchema)
    })
    const [gradingCandidateToBeDeleted, setGradingCandidateToBeDeleted] = useState<GradingCandidate | null>()
    const [gradingCandidate, setGradingCandidate] = useState<GradingCandidate>()
    const [ranks, setRanks] = useState<Array<Rank>>([])

    const gradingCandidateList = usePaginatedList((page, pageSize) => gradingCandidateService.getAll(page, pageSize, { gradingId: props.grading.id }))
    const loadFormOptionsRequest = useLoadRequest()
    const createGradingCandidateRequest = useRequest()
    const updateGradingCandidateRequest = useRequest()
    const deleteGradingCandidateRequest = useRequest()

    // The ranks are the options of the forms
    function loadFormOptionsData() {
        loadFormOptionsRequest.run(
            () => rankService.getAllPages(),
            ranks => setRanks(ranks)
        )
    }

    useOnMount(() => loadFormOptionsData())

    const rankOptions = ranks.map(r => ({
        value: r.id,
        label: `${r.name} - ${r.category.name}`
    }))

    const gradingCandidateRows = gradingCandidateList.items.map(gc => (
        <Table.Tr key={gc.id}>
            <Table.Td>{gc.member.name}</Table.Td>
            <Table.Td>{`${gc.rank.name} - ${gc.rank.category.name}`}</Table.Td>
            <Table.Td>{convertPaymentMethodToView(gc.paymentMethod)}</Table.Td>
            <Table.Td>{convertBooleanToView(gc.isPaid)}</Table.Td>
            <Table.Td>
                <ActionButton label="Ver" icon={<EyeIcon/>} onClick={() => {
                    setGradingCandidate(gc)
                    openViewGradingCandidateModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Editar" icon={<PencilIcon/>} onClick={() => {
                    updateGradingCandidateForm.setValues({
                        id: gc.id,
                        rankId: gc.rank.id,
                        paymentMethod: gc.paymentMethod,
                        isPaid: gc.isPaid
                    })
                    setGradingCandidate(gc)
                    openUpdateGradingCandidateModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setGradingCandidateToBeDeleted(gc)
                    openDeleteGradingCandidateModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    ))

    return (
        <>
            <Button variant="filled" onClick={() => {
                createGradingCandidateForm.reset()
                openCreateGradingCandidateModal()
            }}>Novo</Button>

            <ViewObjectModal
                isViewObjectModalOpen={isViewGradingCandidateModalOpen}
                closeViewObjectModal={closeViewGradingCandidateModal}
                labelValue={gradingCandidate ? convertGradingCandidateToView(gradingCandidate) : []}
            />

            <Modal
                opened={isCreateGradingCandidateModalOpen}
                onClose={() => {
                    closeCreateGradingCandidateModal()
                    createGradingCandidateRequest.clearError()
                }}
                title="Candidato"
                centered
            >
                <form onSubmit={createGradingCandidateForm.onSubmit((values) => createGradingCandidateRequest.run(async () => {
                    await gradingCandidateService.create({ ...values, gradingId: props.grading.id })

                    closeCreateGradingCandidateModal()
                    showSuccessNotification("Candidato salvo")
                    gradingCandidateList.reload()
                }))}>
                    <RequestErrorAlert error={loadFormOptionsRequest.error} onClose={loadFormOptionsRequest.clearError}/>
                    <RequestErrorAlert error={createGradingCandidateRequest.error} onClose={createGradingCandidateRequest.clearError}/>
                    <SearchableCpfSelect
                        search={searchMemberOptionsByCpf}
                        key={createGradingCandidateForm.key('memberId')}
                        {...createGradingCandidateForm.getInputProps('memberId')}
                    />
                    <Select
                        withAsterisk
                        searchable
                        label="Grau pretendido"
                        placeholder="Faixa Azul - Adulto"
                        data={rankOptions}
                        key={createGradingCandidateForm.key('rankId')}
                        {...createGradingCandidateForm.getInputProps('rankId')}
                    />
                    <Select
                        withAsterisk
                        label="Forma de pagamento"
                        placeholder={convertPaymentMethodToView(PaymentMethod.PIX)}
                        data={paymentMethodOptions}
                        key={createGradingCandidateForm.key('paymentMethod')}
                        {...createGradingCandidateForm.getInputProps('paymentMethod')}
                    />
                    <Select
                        withAsterisk
                        label="Pago"
                        placeholder="Não"
                        data={booleanOptions}
                        key={createGradingCandidateForm.key('isPaid')}
                        {...createGradingCandidateForm.getInputProps('isPaid')}
                    />
                    <Button variant="filled" type="submit" loading={createGradingCandidateRequest.isLoading}>Salvar</Button>
                </form>
            </Modal>

            <Modal
                opened={isUpdateGradingCandidateModalOpen}
                onClose={() => {
                    closeUpdateGradingCandidateModal()
                    updateGradingCandidateRequest.clearError()
                }}
                title={`Candidato - ${gradingCandidate?.member.name ?? ""}`}
                centered
            >
                <form onSubmit={updateGradingCandidateForm.onSubmit((values) => updateGradingCandidateRequest.run(async () => {
                    await gradingCandidateService.updateById(values.id, values)

                    closeUpdateGradingCandidateModal()
                    showSuccessNotification("Candidato salvo")
                    gradingCandidateList.reload()
                }))}>
                    <RequestErrorAlert error={updateGradingCandidateRequest.error} onClose={updateGradingCandidateRequest.clearError}/>
                    <Select
                        withAsterisk
                        searchable
                        label="Grau pretendido"
                        placeholder="Faixa Azul - Adulto"
                        data={rankOptions}
                        key={updateGradingCandidateForm.key('rankId')}
                        {...updateGradingCandidateForm.getInputProps('rankId')}
                    />
                    <Select
                        withAsterisk
                        label="Forma de pagamento"
                        placeholder={convertPaymentMethodToView(PaymentMethod.PIX)}
                        data={paymentMethodOptions}
                        key={updateGradingCandidateForm.key('paymentMethod')}
                        {...updateGradingCandidateForm.getInputProps('paymentMethod')}
                    />
                    <Select
                        withAsterisk
                        label="Pago"
                        placeholder="Não"
                        data={booleanOptions}
                        key={updateGradingCandidateForm.key('isPaid')}
                        {...updateGradingCandidateForm.getInputProps('isPaid')}
                    />
                    <Button variant="filled" type="submit" loading={updateGradingCandidateRequest.isLoading}>Salvar</Button>
                </form>
            </Modal>

            <DeleteConfirmModal
                opened={isDeleteGradingCandidateModalOpen}
                onClose={closeDeleteGradingCandidateModal}
                title="Deletar candidato?"
                isLoading={deleteGradingCandidateRequest.isLoading}
                error={deleteGradingCandidateRequest.error}
                onCloseError={deleteGradingCandidateRequest.clearError}
                onConfirm={() => deleteGradingCandidateRequest.run(async () => {
                    if (!gradingCandidateToBeDeleted) {
                        throw new Error("gradingCandidateToBeDeleted is empty")
                    }

                    await gradingCandidateService.deleteById(gradingCandidateToBeDeleted.id)
                    setGradingCandidateToBeDeleted(null)
                    closeDeleteGradingCandidateModal()
                    showSuccessNotification("Candidato deletado")
                    gradingCandidateList.reload()
                })}
            >
                Tem certeza que deseja deletar a inscrição de {gradingCandidateToBeDeleted?.member.name} na graduação {props.grading.name}?
            </DeleteConfirmModal>

            <RequestErrorAlert error={gradingCandidateList.error} onClose={gradingCandidateList.clearError}/>
            <RequestLoader isLoading={gradingCandidateList.isLoading} hasLoaded={gradingCandidateList.hasLoaded}>
                <Table striped mt="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Membro</Table.Th>
                            <Table.Th>Grau pretendido</Table.Th>
                            <Table.Th>Forma de pagamento</Table.Th>
                            <Table.Th>Pago</Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>{gradingCandidateRows.length > 0 ? gradingCandidateRows : <EmptyTableRow colSpan={7}/>}</Table.Tbody>
                </Table>
            </RequestLoader>

            <ListPagination list={gradingCandidateList}/>
        </>
    )
}

interface CreateGradingCandidateForm {
    memberId: string
    rankId: string
    paymentMethod: PaymentMethod
    isPaid: boolean
}

interface UpdateGradingCandidateForm {
    id: string
    rankId: string
    paymentMethod: PaymentMethod
    isPaid: boolean
}
