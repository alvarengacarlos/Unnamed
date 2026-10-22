import { Button, Modal, NumberInput, Select, Table } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { schemaResolver, useForm } from "@mantine/form";
import { useState } from "react";
import z from "zod/v4";
import { EyeIcon, PencilIcon, TrashIcon } from "@phosphor-icons/react";
import { tuitionPaymentService } from "../instance";
import type { TuitionPayment } from "./tuition-payment.type";
import type { StudentEnrollment } from "../student-enrollment/student-enrollment.type";
import { Month } from "../common/common.type";
import { booleanOptions, convertBooleanToView, convertMonthToService, convertMonthToView } from "../common/common.converter";
import { convertReferenceToView, convertTuitionPaymentToView } from "./tuition-payment.converter";
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

const tuitionPaymentFormSchema = z.object({
    month: z.enum(Month, { error: 'Mês é obrigatório' }),
    year: z.number({ error: 'Ano é obrigatório' }).int({ error: 'Ano deve ser inteiro' }).min(2000, { error: 'Ano inválido' }),
    isPaid: z.boolean({ error: 'Pago é obrigatório' })
})

const monthOptions = Object.values(Month).map(month => ({
    value: month,
    label: convertMonthToView(month)
}))

interface Props {
    opened: boolean
    onClose: () => void
    // The enrollment the new payments are for. The payments listed are the ones of its student, in all the clazzes.
    studentEnrollment?: StudentEnrollment
}

export default function TuitionPaymentModal(props: Props) {
    const title = props.studentEnrollment
        ? `Pagamentos - ${props.studentEnrollment.student.member.name} - ${props.studentEnrollment.clazz.name}`
        : "Pagamentos"

    return (
        <Modal opened={props.opened} onClose={props.onClose} title={title} fullScreen>
            {/* The content mounts each time the modal opens, so it starts clean for the enrollment chosen */}
            {props.studentEnrollment && <TuitionPaymentContent studentEnrollment={props.studentEnrollment}/>}
        </Modal>
    )
}

interface ContentProps {
    studentEnrollment: StudentEnrollment
}

function TuitionPaymentContent(props: ContentProps) {
    const [isViewTuitionPaymentModalOpen, { open: openViewTuitionPaymentModal, close: closeViewTuitionPaymentModal }] = useDisclosure(false)
    const [isCreateUpdateTuitionPaymentModalOpen, { open: openCreateUpdateTuitionPaymentModal, close: closeCreateUpdateTuitionPaymentModal }] = useDisclosure(false)
    const [isDeleteTuitionPaymentModalOpen, { open: openDeleteTuitionPaymentModal, close: closeDeleteTuitionPaymentModal }] = useDisclosure(false)

    const tuitionPaymentForm = useForm<TuitionPaymentForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(tuitionPaymentFormSchema)
    })
    const [tuitionPaymentToBeDeleted, setTuitionPaymentToBeDeleted] = useState<TuitionPayment | null>()
    const [tuitionPayment, setTuitionPayment] = useState<TuitionPayment>()

    const tuitionPaymentList = usePaginatedList((page, pageSize) => tuitionPaymentService.getAll(page, pageSize, { studentId: props.studentEnrollment.student.id }))
    const saveTuitionPaymentRequest = useRequest()
    const deleteTuitionPaymentRequest = useRequest()

    const tuitionPaymentRows = tuitionPaymentList.items.map(tp => (
        <Table.Tr key={tp.id}>
            <Table.Td>{tp.studentEnrollment.student.member.name}</Table.Td>
            <Table.Td>{tp.studentEnrollment.clazz.name}</Table.Td>
            <Table.Td>{convertReferenceToView(tp)}</Table.Td>
            <Table.Td>{convertBooleanToView(tp.isPaid)}</Table.Td>
            <Table.Td>
                <ActionButton label="Ver" icon={<EyeIcon/>} onClick={() => {
                    setTuitionPayment(tp)
                    openViewTuitionPaymentModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Editar" icon={<PencilIcon/>} onClick={() => {
                    tuitionPaymentForm.setValues({
                        id: tp.id,
                        month: tp.month,
                        year: tp.year,
                        isPaid: tp.isPaid
                    })
                    openCreateUpdateTuitionPaymentModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setTuitionPaymentToBeDeleted(tp)
                    openDeleteTuitionPaymentModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    ))

    return (
        <>
            <Button variant="filled" onClick={() => {
                tuitionPaymentForm.reset()
                openCreateUpdateTuitionPaymentModal()
            }}>Novo</Button>

            <ViewObjectModal
                isViewObjectModalOpen={isViewTuitionPaymentModalOpen}
                closeViewObjectModal={closeViewTuitionPaymentModal}
                labelValue={tuitionPayment ? convertTuitionPaymentToView(tuitionPayment) : []}
            />

            <Modal
                opened={isCreateUpdateTuitionPaymentModalOpen}
                onClose={() => {
                    closeCreateUpdateTuitionPaymentModal()
                    saveTuitionPaymentRequest.clearError()
                }}
                title="Pagamento"
                centered
            >
                <form onSubmit={tuitionPaymentForm.onSubmit((values) => saveTuitionPaymentRequest.run(async () => {
                    const input = { ...values, month: convertMonthToService(values.month) }
                    if (!values.id) {
                        await tuitionPaymentService.create({ ...input, studentEnrollmentId: props.studentEnrollment.id })
                    } else {
                        await tuitionPaymentService.updateById(values.id, input)
                    }

                    closeCreateUpdateTuitionPaymentModal()
                    showSuccessNotification("Pagamento salvo")
                    tuitionPaymentList.reload()
                }))}>
                    <RequestErrorAlert error={saveTuitionPaymentRequest.error} onClose={saveTuitionPaymentRequest.clearError}/>
                    <Select
                        withAsterisk
                        label="Mês"
                        placeholder={convertMonthToView(Month.JANUARY)}
                        data={monthOptions}
                        key={tuitionPaymentForm.key('month')}
                        {...tuitionPaymentForm.getInputProps('month')}
                    />
                    <NumberInput
                        withAsterisk
                        label="Ano"
                        placeholder={String(new Date().getFullYear())}
                        allowDecimal={false}
                        key={tuitionPaymentForm.key('year')}
                        {...tuitionPaymentForm.getInputProps('year')}
                    />
                    <Select
                        withAsterisk
                        label="Pago"
                        placeholder="Não"
                        data={booleanOptions}
                        key={tuitionPaymentForm.key('isPaid')}
                        {...tuitionPaymentForm.getInputProps('isPaid')}
                    />
                    <Button variant="filled" type="submit" loading={saveTuitionPaymentRequest.isLoading}>Salvar</Button>
                </form>
            </Modal>

            <DeleteConfirmModal
                opened={isDeleteTuitionPaymentModalOpen}
                onClose={closeDeleteTuitionPaymentModal}
                title="Deletar pagamento?"
                isLoading={deleteTuitionPaymentRequest.isLoading}
                error={deleteTuitionPaymentRequest.error}
                onCloseError={deleteTuitionPaymentRequest.clearError}
                onConfirm={() => deleteTuitionPaymentRequest.run(async () => {
                    if (!tuitionPaymentToBeDeleted) {
                        throw new Error("tuitionPaymentToBeDeleted is empty")
                    }

                    await tuitionPaymentService.deleteById(tuitionPaymentToBeDeleted.id)
                    setTuitionPaymentToBeDeleted(null)
                    closeDeleteTuitionPaymentModal()
                    showSuccessNotification("Pagamento deletado")
                    tuitionPaymentList.reload()
                })}
            >
                Tem certeza que deseja deletar o pagamento de {tuitionPaymentToBeDeleted?.studentEnrollment.student.member.name} referente a {tuitionPaymentToBeDeleted ? convertReferenceToView(tuitionPaymentToBeDeleted) : ""}?
            </DeleteConfirmModal>

            <RequestErrorAlert error={tuitionPaymentList.error} onClose={tuitionPaymentList.clearError}/>
            <RequestLoader isLoading={tuitionPaymentList.isLoading} hasLoaded={tuitionPaymentList.hasLoaded}>
                <Table striped mt="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Aluno</Table.Th>
                            <Table.Th>Turma</Table.Th>
                            <Table.Th>Referente a</Table.Th>
                            <Table.Th>Pago</Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>{tuitionPaymentRows.length > 0 ? tuitionPaymentRows : <EmptyTableRow colSpan={7}/>}</Table.Tbody>
                </Table>
            </RequestLoader>

            <ListPagination list={tuitionPaymentList}/>
        </>
    )
}

interface TuitionPaymentForm {
    id: string
    month: Month
    year: number
    isPaid: boolean
}
