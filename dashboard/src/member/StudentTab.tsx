import { Modal, Table } from "@mantine/core"
import { useDisclosure } from "@mantine/hooks";
import { useState } from "react";
import { BookIcon, CoinsIcon } from "@phosphor-icons/react";
import { studentEnrollmentService, studentService } from "../instance";
import type { StudentEnrollment } from "../student-enrollment/student-enrollment.type";
import { convertBooleanToView } from "../common/common.converter";
import { convertStudentToView } from "./member.converter";
import { convertMartialArtToView, convertScheduleToView, convertWeekdaysToView } from "../clazz/clazz.converter";
import { useLoadRequest } from "../common/hook/useRequest.hook";
import RequestErrorAlert from "../common/component/RequestErrorAlert.component";
import RequestLoader from "../common/component/RequestLoader.component";
import ActionButton from "../common/component/ActionButton.component";
import EmptyTableRow from "../common/component/EmptyTableRow.component";
import TuitionPaymentModal from "../tuition-payment/TuitionPaymentModal";
import MemberRoleTab from "./MemberRoleTab";

// The tab of a role of a member, plus the enrollments of each student and their tuition payments
export default function StudentTab() {
    const [isStudentEnrollmentModalOpen, { open: openStudentEnrollmentModal, close: closeStudentEnrollmentModal }] = useDisclosure(false);
    const [isTuitionPaymentModalOpen, { open: openTuitionPaymentModal, close: closeTuitionPaymentModal }] = useDisclosure(false);

    const [studentEnrollments, setStudentEnrollments] = useState<Array<StudentEnrollment>>([])
    const [studentEnrollment, setStudentEnrollment] = useState<StudentEnrollment>()

    const loadStudentEnrollmentRequest = useLoadRequest()

    function loadStudentEnrollmentData(studentId: string) {
        loadStudentEnrollmentRequest.run(
            () => studentEnrollmentService.getAllPages({ studentId }),
            studentEnrollments => setStudentEnrollments(studentEnrollments)
        )
    }

    const studentEnrollmentRows = studentEnrollments.map(se =>
        <Table.Tr key={se.id}>
            <Table.Td>{se.clazz.name}</Table.Td>
            <Table.Td>{convertMartialArtToView(se.clazz.martialArt)}</Table.Td>
            <Table.Td>{convertBooleanToView(se.isActive)}</Table.Td>
            <Table.Td>{convertWeekdaysToView(se.clazz)}</Table.Td>
            <Table.Td>{convertScheduleToView(se.clazz)}</Table.Td>
            <Table.Td>
                <ActionButton label="Pagamentos" icon={<CoinsIcon/>} onClick={() => {
                    setStudentEnrollment(se)
                    openTuitionPaymentModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    )

    return (
        <>
            <MemberRoleTab
                service={studentService}
                title="Aluno"
                convertToView={convertStudentToView}
                viewTestId="student-view-modal"
                actions={[{
                    label: "Matrículas",
                    icon: <BookIcon/>,
                    onClick: student => {
                        setStudentEnrollments([])
                        loadStudentEnrollmentData(student.id)
                        openStudentEnrollmentModal()
                    }
                }]}
            />

            <Modal
                opened={isStudentEnrollmentModalOpen}
                onClose={() => {
                    closeStudentEnrollmentModal()
                    loadStudentEnrollmentRequest.clearError()
                }}
                title="Matrículas"
                fullScreen
            >
                {/* Inside the modal of the enrollments, so it opens on top of it */}
                <TuitionPaymentModal
                    opened={isTuitionPaymentModalOpen}
                    onClose={closeTuitionPaymentModal}
                    studentEnrollment={studentEnrollment}
                />

                <RequestErrorAlert error={loadStudentEnrollmentRequest.error} onClose={loadStudentEnrollmentRequest.clearError}/>
                {/* Always the Loader while loading, so the enrollments of the student seen before do not show */}
                <RequestLoader isLoading={loadStudentEnrollmentRequest.isLoading} hasLoaded={false}>
                    <Table>
                        <Table.Thead>
                            <Table.Tr>
                                <Table.Th>Turma</Table.Th>
                                <Table.Th>Arte marcial</Table.Th>
                                <Table.Th>Ativa</Table.Th>
                                <Table.Th>Dias da semana</Table.Th>
                                <Table.Th>Horário</Table.Th>
                                <Table.Th></Table.Th>
                            </Table.Tr>
                        </Table.Thead>
                        <Table.Tbody>{studentEnrollmentRows.length > 0 ? studentEnrollmentRows : <EmptyTableRow colSpan={6}/>}</Table.Tbody>
                    </Table>
                </RequestLoader>
            </Modal>
        </>
    )
}
