import { schemaResolver, useForm } from "@mantine/form";
import { useDisclosure } from "@mantine/hooks";
import { useState } from "react";
import z from "zod/v4";
import { requiredText } from "../common/common.schema";
import type { Tuition as TuitionT } from "./tuition.type";
import { tuitionService } from "../instance";
import { Button, Modal, NumberInput, Table, Tabs, TextInput } from "@mantine/core";
import { EyeIcon, PencilIcon, TrashIcon } from "@phosphor-icons/react";
import ViewObjectModal from "../common/component/ViewObjectModal.component";
import { convertTuitionToView } from "./tuition.converter";
import { convertPriceToView } from "../common/common.converter";
import { useRequest } from "../common/hook/useRequest.hook";
import { usePaginatedList } from "../common/hook/usePaginatedList.hook";
import { showSuccessNotification } from "../common/common.notification";
import RequestErrorAlert from "../common/component/RequestErrorAlert.component";
import RequestLoader from "../common/component/RequestLoader.component";
import ActionButton from "../common/component/ActionButton.component";
import DeleteConfirmModal from "../common/component/DeleteConfirmModal.component";
import EmptyTableRow from "../common/component/EmptyTableRow.component";
import ListPagination from "../common/component/ListPagination.component";

const tuitionFormSchema = z.object({
    name: requiredText('Nome é obrigatório'),
    price: z.number({error: 'Preço é obrigatório'}).nonnegative({error: 'Precisa ser não negativo'}),
})

export default function Tuition() {
    const [isViewTuitionModalOpen, { open: openViewTuitionModal, close: closeViewTuitionModal }] = useDisclosure(false);
    const [isCreateUpdateTuitionModalOpen, { open: openCreateUpdateTuitionModal, close: closeCreateUpdateTuitionModal }] = useDisclosure(false);
    const [isDeleteTuitionModalOpen, { open: openDeleteTuitionModal, close: closeDeleteTuitionModal }] = useDisclosure(false);

    const tuitionForm = useForm<TuitionForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(tuitionFormSchema)
    })
    const [tuitionToBeDeleted, setTuitionToBeDeleted] = useState<TuitionT | null>()

    const [tuition, setTuition] = useState<TuitionT>()

    const tuitionList = usePaginatedList((page, pageSize) => tuitionService.getAll(page, pageSize))
    const saveTuitionRequest = useRequest()
    const deleteTuitionRequest = useRequest()

    const tuitionRows = tuitionList.items.map((t) => (
        <Table.Tr key={t.id}>
            <Table.Td>{t.name}</Table.Td>
            <Table.Td>{convertPriceToView(t.price)}</Table.Td>
            <Table.Td>
                <ActionButton label="Ver" icon={<EyeIcon/>} onClick={() => {
                    setTuition(t)
                    openViewTuitionModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Editar" icon={<PencilIcon/>} onClick={() => {
                    tuitionForm.setValues({
                        id: t.id,
                        name: t.name,
                        price: t.price,
                    })
                    openCreateUpdateTuitionModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setTuitionToBeDeleted(t)
                    openDeleteTuitionModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    ))

    return (
        <Tabs defaultValue="tuitions">
            <Tabs.List>
                <Tabs.Tab value="tuitions">Mensalidades</Tabs.Tab>
            </Tabs.List>

            <Tabs.Panel value="tuitions" pt="md">
                <Button variant="filled" onClick={() => {
                    tuitionForm.reset()
                    openCreateUpdateTuitionModal()
                }}>Novo</Button>

                <ViewObjectModal
                    isViewObjectModalOpen={isViewTuitionModalOpen}
                    closeViewObjectModal={closeViewTuitionModal}
                    labelValue={tuition ? convertTuitionToView(tuition) : []}
                />

                <Modal
                    opened={isCreateUpdateTuitionModalOpen}
                    onClose={() => {
                        closeCreateUpdateTuitionModal()
                        saveTuitionRequest.clearError()
                    }}
                    title="Mensalidade"
                    centered
                >
                    <form onSubmit={tuitionForm.onSubmit((values) => saveTuitionRequest.run(async () => {
                            if (!values.id) {
                                await tuitionService.create(values)
                            } else {
                                await tuitionService.updateById(values.id, values)
                            }

                            closeCreateUpdateTuitionModal()
                            showSuccessNotification("Mensalidade salva")
                            tuitionList.reload()
                    }))}>
                        <RequestErrorAlert error={saveTuitionRequest.error} onClose={saveTuitionRequest.clearError}/>
                        <TextInput
                            withAsterisk
                            label="Nome"
                            placeholder="Plano Simples"
                            key={tuitionForm.key('name')}
                            {...tuitionForm.getInputProps('name')}
                        />
                        <NumberInput
                            withAsterisk
                            label="Preço"
                            placeholder="R$ 100,00"
                            prefix="R$ "
                            decimalSeparator=","
                            thousandSeparator="."
                            decimalScale={2}
                            fixedDecimalScale
                            key={tuitionForm.key('price')}
                            {...tuitionForm.getInputProps('price')}
                        />
                        <Button variant="filled" type="submit" loading={saveTuitionRequest.isLoading}>Salvar</Button>
                    </form>
                </Modal>

                <DeleteConfirmModal
                    opened={isDeleteTuitionModalOpen}
                    onClose={closeDeleteTuitionModal}
                    title="Deletar mensalidade?"
                    isLoading={deleteTuitionRequest.isLoading}
                    error={deleteTuitionRequest.error}
                    onCloseError={deleteTuitionRequest.clearError}
                    onConfirm={() => deleteTuitionRequest.run(async () => {
                        if (!tuitionToBeDeleted) {
                            throw new Error("tuitionToBeDeleted is empty")
                        }

                        await tuitionService.deleteById(tuitionToBeDeleted.id)
                        setTuitionToBeDeleted(null)
                        closeDeleteTuitionModal()
                        showSuccessNotification("Mensalidade deletada")
                        tuitionList.reload()
                    })}
                >
                    Tem certeza que deseja deletar a mensalidade {tuitionToBeDeleted?.name}?
                </DeleteConfirmModal>

                <RequestErrorAlert error={tuitionList.error} onClose={tuitionList.clearError}/>
                <RequestLoader isLoading={tuitionList.isLoading} hasLoaded={tuitionList.hasLoaded}>
                    <Table striped mt="md">
                        <Table.Thead>
                            <Table.Tr>
                                <Table.Th>Nome</Table.Th>
                                <Table.Th>Preço</Table.Th>
                                <Table.Th></Table.Th>
                                <Table.Th></Table.Th>
                                <Table.Th></Table.Th>
                            </Table.Tr>
                        </Table.Thead>
                        <Table.Tbody>{tuitionRows.length > 0 ? tuitionRows : <EmptyTableRow colSpan={5}/>}</Table.Tbody>
                    </Table>
                </RequestLoader>

                <ListPagination list={tuitionList}/>
            </Tabs.Panel>
        </Tabs>
    )
}

interface TuitionForm {
    id: string
    name: string
    price: number
}
