import { Button, Modal, Select, Table } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { schemaResolver, useForm } from "@mantine/form";
import { useState } from "react";
import z from "zod/v4";
import { PencilIcon, TrashIcon } from "@phosphor-icons/react";
import { studentEnrollmentService } from "../instance";
import type { StudentEnrollment } from "./student-enrollment.type";
import type { Clazz } from "../clazz/clazz.type";
import type { Tuition } from "../tuition/tuition.type";
import { convertBooleanToView } from "../common/common.converter";
import { useLoadRequest, useOnMount, useRequest } from "../common/hook/useRequest.hook";
import { showSuccessNotification } from "../common/common.notification";
import RequestErrorAlert from "../common/component/RequestErrorAlert.component";
import RequestLoader from "../common/component/RequestLoader.component";
import ActionButton from "../common/component/ActionButton.component";
import DeleteConfirmModal from "../common/component/DeleteConfirmModal.component";
import EmptyTableRow from "../common/component/EmptyTableRow.component";
import SearchableCpfSelect from "../common/component/SearchableCpfSelect.component";
import { searchStudentOptionsByCpf } from "../member/member.search";

const createStudentEnrollmentFormSchema = z.object({
    tuitionId: z.uuid({error: 'Mensalidade é obrigatório'}),
    studentId: z.uuid({error: 'Aluno é obrigatório'}),
})

const updateStudentEnrollmentFormSchema = z.object({
    isActive: z.boolean({ error: 'Ativo é obrigatório'}),
    tuitionId: z.uuid({error: 'Mensalidade é obrigatório'})
})

interface Props {
    opened: boolean
    onClose: () => void
    clazz?: Clazz
    // The options of the tuition of an enrollment
    tuitions: Array<Tuition>
}

// The enrollments of the students in a clazz
export default function StudentEnrollmentModal(props: Props) {
    return (
        <Modal opened={props.opened} onClose={props.onClose} title={`Matrículas - ${props.clazz?.name ?? ""}`} fullScreen>
            {/* The content mounts each time the modal opens, so it loads the enrollments of the clazz chosen */}
            {props.clazz && <StudentEnrollmentContent clazz={props.clazz} tuitions={props.tuitions}/>}
        </Modal>
    )
}

interface ContentProps {
    clazz: Clazz
    tuitions: Array<Tuition>
}

function StudentEnrollmentContent(props: ContentProps) {
    const [isCreateStudentEnrollmentModalOpen, { open: openCreateStudentEnrollmentModal, close: closeCreateStudentEnrollmentModal }] = useDisclosure(false);
    const [isUpdateStudentEnrollmentModalOpen, { open: openUpdateStudentEnrollmentModal, close: closeUpdateStudentEnrollmentModal }] = useDisclosure(false);
    const [isDeleteStudentEnrollmentModalOpen, { open: openDeleteStudentEnrollmentModal, close: closeDeleteStudentEnrollmentModal }] = useDisclosure(false);

    const createStudentEnrollmentForm = useForm<CreateStudentEnrollmentForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(createStudentEnrollmentFormSchema)
    })
    const updateStudentEnrollmentForm = useForm<UpdateStudentEnrollmentForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(updateStudentEnrollmentFormSchema)
    })
    const [studentEnrollmentToBeDeleted, setStudentEnrollmentToBeDeleted] = useState<StudentEnrollment | null>()

    const [studentEnrollments, setStudentEnrollments] = useState<Array<StudentEnrollment>>([])

    const loadStudentEnrollmentRequest = useLoadRequest()
    const createStudentEnrollmentRequest = useRequest()
    const updateStudentEnrollmentRequest = useRequest()
    const deleteStudentEnrollmentRequest = useRequest()

    function loadStudentEnrollmentData() {
        loadStudentEnrollmentRequest.run(
            () => studentEnrollmentService.getAllPages({ clazzId: props.clazz.id }),
            studentEnrollments => setStudentEnrollments(studentEnrollments)
        )
    }

    useOnMount(loadStudentEnrollmentData)

    const tuitionOptions = props.tuitions.map(t => ({
        value: t.id,
        label: t.name
    }))

    const studentEnrollmentRows = studentEnrollments.map((se) => (
        <Table.Tr key={se.id}>
            <Table.Td>{se.student.member.name}</Table.Td>
            <Table.Td>{se.student.member.cpf}</Table.Td>
            <Table.Td>{convertBooleanToView(se.isActive)}</Table.Td>
            <Table.Td>
                <ActionButton label="Editar" icon={<PencilIcon/>} onClick={() => {
                    updateStudentEnrollmentForm.setValues({
                        id: se.id,
                        isActive: se.isActive,
                        tuitionId: se.tuition.id,
                    })
                    openUpdateStudentEnrollmentModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setStudentEnrollmentToBeDeleted(se)
                    openDeleteStudentEnrollmentModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    ))

    return (
        <>
            <Button variant="filled" onClick={() => {
                createStudentEnrollmentForm.reset()
                openCreateStudentEnrollmentModal()
            }}>Novo</Button>

            <Modal
                opened={isCreateStudentEnrollmentModalOpen}
                onClose={() => {
                    closeCreateStudentEnrollmentModal()
                    createStudentEnrollmentRequest.clearError()
                }}
                title="Matrícula"
                centered
            >
                <form onSubmit={createStudentEnrollmentForm.onSubmit((values) => createStudentEnrollmentRequest.run(async () => {
                    await studentEnrollmentService.create({...values, clazzId: props.clazz.id})

                    closeCreateStudentEnrollmentModal()
                    showSuccessNotification("Matrícula salva")
                    loadStudentEnrollmentData()
                }))}>
                    <RequestErrorAlert error={createStudentEnrollmentRequest.error} onClose={createStudentEnrollmentRequest.clearError}/>
                    <SearchableCpfSelect
                        search={searchStudentOptionsByCpf}
                        key={createStudentEnrollmentForm.key("studentId")}
                        {...createStudentEnrollmentForm.getInputProps("studentId")}
                    />
                    <Select
                        withAsterisk
                        label="Mensalidade"
                        placeholder="Plano A"
                        data={tuitionOptions}
                        key={createStudentEnrollmentForm.key('tuitionId')}
                        {...createStudentEnrollmentForm.getInputProps('tuitionId')}
                    />
                    <Button variant="filled" type="submit" loading={createStudentEnrollmentRequest.isLoading}>Salvar</Button>
                </form>
            </Modal>

            <Modal
                opened={isUpdateStudentEnrollmentModalOpen}
                onClose={() => {
                    closeUpdateStudentEnrollmentModal()
                    updateStudentEnrollmentRequest.clearError()
                }}
                title="Matrícula"
                centered
            >
                <form onSubmit={updateStudentEnrollmentForm.onSubmit((values) => updateStudentEnrollmentRequest.run(async () => {
                    await studentEnrollmentService.updateById(values.id, values)

                    closeUpdateStudentEnrollmentModal()
                    showSuccessNotification("Matrícula salva")
                    loadStudentEnrollmentData()
                }))}>
                    <RequestErrorAlert error={updateStudentEnrollmentRequest.error} onClose={updateStudentEnrollmentRequest.clearError}/>
                    <Select
                        withAsterisk
                        label="Ativa"
                        placeholder="Sim"
                        data={[
                            {
                                value: true,
                                label: convertBooleanToView(true)
                            },
                            {
                                value: false,
                                label: convertBooleanToView(false)
                            },
                        ]}
                        key={updateStudentEnrollmentForm.key('isActive')}
                        {...updateStudentEnrollmentForm.getInputProps('isActive')}
                    />
                    <Select
                        withAsterisk
                        label="Mensalidade"
                        placeholder="Plano A"
                        data={tuitionOptions}
                        key={updateStudentEnrollmentForm.key('tuitionId')}
                        {...updateStudentEnrollmentForm.getInputProps('tuitionId')}
                    />
                    <Button variant="filled" type="submit" loading={updateStudentEnrollmentRequest.isLoading}>Salvar</Button>
                </form>
            </Modal>

            <DeleteConfirmModal
                opened={isDeleteStudentEnrollmentModalOpen}
                onClose={closeDeleteStudentEnrollmentModal}
                title="Deletar matrícula?"
                isLoading={deleteStudentEnrollmentRequest.isLoading}
                error={deleteStudentEnrollmentRequest.error}
                onCloseError={deleteStudentEnrollmentRequest.clearError}
                onConfirm={() => deleteStudentEnrollmentRequest.run(async () => {
                    if (!studentEnrollmentToBeDeleted) {
                        throw new Error("studentEnrollmentToBeDeleted is empty")
                    }

                    await studentEnrollmentService.deleteById(studentEnrollmentToBeDeleted.id)
                    setStudentEnrollmentToBeDeleted(null)
                    closeDeleteStudentEnrollmentModal()
                    showSuccessNotification("Matrícula deletada")
                    loadStudentEnrollmentData()
                })}
            >
                Tem certeza que deseja deletar a matrícula de {studentEnrollmentToBeDeleted?.student.member.name}?
            </DeleteConfirmModal>

            <RequestErrorAlert error={loadStudentEnrollmentRequest.error} onClose={loadStudentEnrollmentRequest.clearError}/>
            <RequestLoader isLoading={loadStudentEnrollmentRequest.isLoading} hasLoaded={loadStudentEnrollmentRequest.hasLoaded}>
                <Table mt="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Nome</Table.Th>
                            <Table.Th>CPF</Table.Th>
                            <Table.Th>Ativa</Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>{studentEnrollmentRows.length > 0 ? studentEnrollmentRows : <EmptyTableRow colSpan={5}/>}</Table.Tbody>
                </Table>
            </RequestLoader>
        </>
    )
}

interface CreateStudentEnrollmentForm {
    tuitionId: string
    studentId: string
}

interface UpdateStudentEnrollmentForm {
    id: string
    isActive: boolean
    tuitionId: string
}
