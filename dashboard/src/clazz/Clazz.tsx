import z from "zod/v4";
import { requiredText } from "../common/common.schema";
import { DayOfWeek } from "../common/common.type";
import { MartialArt } from "./clazz.type";
import { schemaResolver, useForm } from "@mantine/form";
import { useState } from "react";
import {Clazz as ClazzT} from "./clazz.type"
import { clazzService, teacherService, tuitionService } from "../instance";
import { Button, Group, Modal, MultiSelect, Select, Table, Tabs, TextInput } from "@mantine/core";
import { TimeInput } from "@mantine/dates";
import { BookIcon, EyeIcon, PencilIcon, TrashIcon } from "@phosphor-icons/react";
import { useDisclosure } from "@mantine/hooks";
import type { Teacher } from "../member/member.type";
import ViewObjectModal from "../common/component/ViewObjectModal.component";
import { convertDayOfWeekToView } from "../common/common.converter";
import { convertClazzToView, convertMartialArtToView, convertScheduleToView, convertTimeToView } from "./clazz.converter";
import type { Tuition } from "../tuition/tuition.type";
import { useLoadRequest, useOnMount, useRequest } from "../common/hook/useRequest.hook";
import { usePaginatedList } from "../common/hook/usePaginatedList.hook";
import { showSuccessNotification } from "../common/common.notification";
import RequestErrorAlert from "../common/component/RequestErrorAlert.component";
import RequestLoader from "../common/component/RequestLoader.component";
import ActionButton from "../common/component/ActionButton.component";
import DeleteConfirmModal from "../common/component/DeleteConfirmModal.component";
import EmptyTableRow from "../common/component/EmptyTableRow.component";
import ListPagination from "../common/component/ListPagination.component";
import StudentEnrollmentModal from "../student-enrollment/StudentEnrollmentModal";

// The time of the TimeInput, without the seconds: the API accepts it and the classes start at whole minutes
const timeRegex = /^(?:[01]\d|2[0-3]):[0-5]\d$/
const clazzFormSchema = z.object({
    name: requiredText('Nome é obrigatório'),
    martialArt: z.enum(MartialArt, {error: 'Arte marcial é obrigatório'}),
    startTime: z.string({error: 'Hora inicial é obrigatório'}).regex(timeRegex, {error: 'Hora inicial é obrigatório'}),
    endTime: z.string({error: 'Hora final é obrigatório'}).regex(timeRegex, {error: 'Hora final é obrigatório'}),
    weekdays: z.array(z.enum(DayOfWeek), {error: 'Dias da semana é obrigatório'}).min(1, {error: 'Selecione ao menos 1 dia'}),
    teacherId: z.uuid({error: 'Professor é obrigatório'})
})

// From Sunday, as the calendar of the user
const weekdayOptions = [
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY
].map(weekday => ({
    value: weekday,
    label: convertDayOfWeekToView(weekday)
}))

const martialArtOptions = Object.values(MartialArt).map(martialArt => ({
    value: martialArt,
    label: convertMartialArtToView(martialArt)
}))

export default function Clazz() {
    const [isViewClazzModalOpen, { open: openViewClazzModal, close: closeViewClazzModal }] = useDisclosure(false);
    const [isCreateUpdateClazzModalOpen, { open: openCreateUpdateClazzModal, close: closeCreateUpdateClazzModal }] = useDisclosure(false);
    const [isDeleteClazzModalOpen, { open: openDeleteClazzModal, close: closeDeleteClazzModal }] = useDisclosure(false);
    const [isStudentEnrollmentModalOpen, { open: openStudentEnrollmentModal, close: closeStudentEnrollmentModal }] = useDisclosure(false);

    const clazzForm = useForm<ClazzForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(clazzFormSchema)
    })
    const [clazzToBeDeleted, setClazzToBeDeleted] = useState<ClazzT | null>()

    const [clazz, setClazz] = useState<ClazzT>()
    const [teachers, setTeachers] = useState<Array<Teacher>>([])
    const [tuitions, setTuitions] = useState<Array<Tuition>>([])

    // The filter is the martial art; null lists the ones of all of them
    const clazzList = usePaginatedList(
        (page, pageSize, martialArt: MartialArt | null) => clazzService.getAll(page, pageSize, { martialArt: martialArt ?? undefined }),
        null
    )
    const loadFormOptionsRequest = useLoadRequest()
    const saveClazzRequest = useRequest()
    const deleteClazzRequest = useRequest()

    // The teachers and the tuitions are the options of the forms
    function loadFormOptionsData(): void {
        loadFormOptionsRequest.run(
            () => Promise.all([
                teacherService.getAllPages(),
                tuitionService.getAllPages()
            ]),
            ([teachers, tuitions]) => {
                setTeachers(teachers)
                setTuitions(tuitions)
            }
        )
    }

    useOnMount(() => loadFormOptionsData())

    const clazzRows = clazzList.items.map((c) => (
        <Table.Tr key={c.id}>
            <Table.Td>{c.name}</Table.Td>
            <Table.Td>{convertMartialArtToView(c.martialArt)}</Table.Td>
            <Table.Td>{convertScheduleToView(c)}</Table.Td>
            <Table.Td>{c.teacher.member.name}</Table.Td>
            <Table.Td>
                <ActionButton label="Matrículas" icon={<BookIcon/>} onClick={() => {
                    setClazz(c)
                    openStudentEnrollmentModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Ver" icon={<EyeIcon/>} onClick={() => {
                    setClazz(c)
                    openViewClazzModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Editar" icon={<PencilIcon/>} onClick={() => {
                    clazzForm.setValues({
                        id: c.id,
                        name: c.name,
                        martialArt: c.martialArt,
                        startTime: convertTimeToView(c.startTime),
                        endTime: convertTimeToView(c.endTime),
                        weekdays: c.weekdays,
                        teacherId: c.teacher.id
                    })
                    openCreateUpdateClazzModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setClazzToBeDeleted(c)
                    openDeleteClazzModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    ))

    return (
        <Tabs defaultValue="clazzes">
            <Tabs.List>
                <Tabs.Tab value="clazzes">Turmas</Tabs.Tab>
            </Tabs.List>

            <Tabs.Panel value="clazzes" pt="md">
                <Group justify="space-between">
                    <Button variant="filled" onClick={() => {
                        clazzForm.reset()
                        openCreateUpdateClazzModal()
                    }}>Novo</Button>
                    <Select
                        clearable
                        aria-label="Arte marcial"
                        placeholder="Todas as artes marciais"
                        data={martialArtOptions}
                        value={clazzList.filter}
                        onChange={(value) => clazzList.changeFilter(value as MartialArt | null)}
                    />
                </Group>

                <StudentEnrollmentModal
                    opened={isStudentEnrollmentModalOpen}
                    onClose={closeStudentEnrollmentModal}
                    clazz={clazz}
                    tuitions={tuitions}
                />

                <ViewObjectModal
                    isViewObjectModalOpen={isViewClazzModalOpen}
                    closeViewObjectModal={closeViewClazzModal}
                    labelValue={clazz ? convertClazzToView(clazz) : []}
                />

                <Modal
                    opened={isCreateUpdateClazzModalOpen}
                    onClose={() => {
                        closeCreateUpdateClazzModal()
                        saveClazzRequest.clearError()
                    }}
                    title="Turma"
                    centered
                >
                    <form onSubmit={clazzForm.onSubmit((values) => saveClazzRequest.run(async () => {
                            if (!values.id) {
                                await clazzService.create(values)
                            } else {
                                await clazzService.updateById(values.id, values)
                            }

                            closeCreateUpdateClazzModal()
                            showSuccessNotification("Turma salva")
                            clazzList.reload()
                    }))}>
                        <RequestErrorAlert error={saveClazzRequest.error} onClose={saveClazzRequest.clearError}/>
                        <TextInput
                            withAsterisk
                            label="Nome"
                            placeholder="Turma Muay Thai - Manhã"
                            key={clazzForm.key('name')}
                            {...clazzForm.getInputProps('name')}
                        />
                        <Select
                            withAsterisk
                            label="Arte marcial"
                            placeholder="Muay Thai"
                            data={martialArtOptions}
                            key={clazzForm.key('martialArt')}
                            {...clazzForm.getInputProps('martialArt')}
                        />
                        <TimeInput
                            withAsterisk
                            label="Hora inicial"
                            key={clazzForm.key('startTime')}
                            {...clazzForm.getInputProps('startTime')}
                        />
                        <TimeInput
                            withAsterisk
                            label="Hora final"
                            key={clazzForm.key('endTime')}
                            {...clazzForm.getInputProps('endTime')}
                        />
                        <MultiSelect
                            withAsterisk
                            label="Dias da semana"
                            placeholder={convertDayOfWeekToView(DayOfWeek.MONDAY)}
                            data={weekdayOptions}
                            key={clazzForm.key('weekdays')}
                            {...clazzForm.getInputProps('weekdays')}
                        />
                        <Select
                            withAsterisk
                            label="Professor"
                            placeholder="João Da Silva"
                            data={
                                teachers.map(t => ({
                                    value: t.id,
                                    label: t.member.name
                                }))
                            }
                            key={clazzForm.key('teacherId')}
                            {...clazzForm.getInputProps('teacherId')}
                        />
                        <Button variant="filled" type="submit" loading={saveClazzRequest.isLoading}>Salvar</Button>
                    </form>
                </Modal>

                <DeleteConfirmModal
                    opened={isDeleteClazzModalOpen}
                    onClose={closeDeleteClazzModal}
                    title="Deletar turma?"
                    isLoading={deleteClazzRequest.isLoading}
                    error={deleteClazzRequest.error}
                    onCloseError={deleteClazzRequest.clearError}
                    onConfirm={() => deleteClazzRequest.run(async () => {
                        if (!clazzToBeDeleted) {
                            throw new Error("clazzToBeDeleted is empty")
                        }

                        await clazzService.deleteById(clazzToBeDeleted.id)
                        setClazzToBeDeleted(null)
                        closeDeleteClazzModal()
                        showSuccessNotification("Turma deletada")
                        clazzList.reload()
                    })}
                >
                    Tem certeza que deseja deletar a turma {clazzToBeDeleted?.name}?
                </DeleteConfirmModal>

                <RequestErrorAlert error={loadFormOptionsRequest.error} onClose={loadFormOptionsRequest.clearError}/>
                <RequestErrorAlert error={clazzList.error} onClose={clazzList.clearError}/>
                <RequestLoader isLoading={clazzList.isLoading} hasLoaded={clazzList.hasLoaded}>
                    <Table striped mt="md">
                        <Table.Thead>
                            <Table.Tr>
                                <Table.Th>Nome</Table.Th>
                                <Table.Th>Arte marcial</Table.Th>
                                <Table.Th>Horário</Table.Th>
                                <Table.Th>Professor</Table.Th>
                                <Table.Th></Table.Th>
                                <Table.Th></Table.Th>
                                <Table.Th></Table.Th>
                                <Table.Th></Table.Th>
                            </Table.Tr>
                        </Table.Thead>
                        <Table.Tbody>{clazzRows.length > 0 ? clazzRows : <EmptyTableRow colSpan={8}/>}</Table.Tbody>
                    </Table>
                </RequestLoader>

                <ListPagination list={clazzList}/>
            </Tabs.Panel>
        </Tabs>
    )
}

interface ClazzForm {
    id: string
    name: string
    martialArt: MartialArt
    startTime: string
    endTime: string
    weekdays: Array<DayOfWeek>
    teacherId: string
}
