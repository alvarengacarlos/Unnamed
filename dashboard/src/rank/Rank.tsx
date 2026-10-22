import { Button, Modal, NumberInput, Select, Table, Tabs, TextInput } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import {z} from 'zod/v4'
import { requiredText } from '../common/common.schema'
import type { RankCategory, Rank as RankT } from "./rank.type";
import { EyeIcon, PencilIcon, TrashIcon } from "@phosphor-icons/react";
import { schemaResolver, useForm } from "@mantine/form";
import { useState } from "react";
import { rankCategoryService, rankService } from "../instance";
import ViewObjectModal from "../common/component/ViewObjectModal.component";
import { convertRankCategoryToView, convertRankToView } from "./rank.converter";
import { useLoadRequest, useOnMount, useRequest } from "../common/hook/useRequest.hook";
import { usePaginatedList } from "../common/hook/usePaginatedList.hook";
import { showSuccessNotification } from "../common/common.notification";
import RequestErrorAlert from "../common/component/RequestErrorAlert.component";
import RequestLoader from "../common/component/RequestLoader.component";
import ActionButton from "../common/component/ActionButton.component";
import DeleteConfirmModal from "../common/component/DeleteConfirmModal.component";
import EmptyTableRow from "../common/component/EmptyTableRow.component";
import ListPagination from "../common/component/ListPagination.component";

const ALL_RANK_CATEGORIES = 'Todas'

const rankFormSchema = z.object({
    name: requiredText('Nome é obrigatório'),
    order: z.number({error: 'Ordem é obrigatório'}).positive({error: 'Precisa ser positivo'}),
    categoryId: z.uuid({error: 'Categoria é obrigatório'})
})

const rankCategorySchema = z.object({
    name: requiredText('Nome é obrigatório'),
})

export default function Rank() {
    const [isViewRankModalOpen, { open: openViewRankModal, close: closeViewRankModal }] = useDisclosure(false);
    const [isCreateUpdateRankModalOpen, { open: openCreateUpdateRankModal, close: closeCreateUpdateRankModal }] = useDisclosure(false);
    const [isDeleteRankModalOpen, { open: openDeleteRankModal, close: closeDeleteRankModal }] = useDisclosure(false);

    const [isViewRankCategoryModalOpen, { open: openViewRankCategoryModal, close: closeViewRankCategoryModal }] = useDisclosure(false);
    const [isCreateUpdateRankCategoryModalOpen, { open: openCreateUpdateRankCategoryModal, close: closeCreateUpdateRankCategoryModal }] = useDisclosure(false);
    const [isDeleteRankCategoryModalOpen, { open: openDeleteRankCategoryModal, close: closeDeleteRankCategoryModal }] = useDisclosure(false);

    const rankForm = useForm<RankForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(rankFormSchema)
    })
    const [rankToBeDeleted, setRankToBeDeleted] = useState<RankT | null>()

    const rankCategoryForm = useForm<RankCategoryForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(rankCategorySchema)
    });
    const [rankCategoryToBeDeleted, setRankCategoryToBeDeleted] = useState<RankCategory | null>()

    const [rank, setRank] = useState<RankT>()
    // All the categories, the options of the filter and of the rank form
    const [rankCategoryOptions, setRankCategoryOptions] = useState<Array<RankCategory>>([])
    const [rankCategory, setRankCategory] = useState<RankCategory>()

    // The filter is the id of the category; null lists the ranks of all of them
    const rankList = usePaginatedList(
        (page, pageSize, categoryId: string | null) => rankService.getAll(page, pageSize, { categoryId: categoryId ?? undefined }),
        null
    )
    const saveRankRequest = useRequest()
    const deleteRankRequest = useRequest()

    const rankCategoryList = usePaginatedList((page, pageSize) => rankCategoryService.getAll(page, pageSize))
    const loadRankCategoryOptionsRequest = useLoadRequest()
    const saveRankCategoryRequest = useRequest()
    const deleteRankCategoryRequest = useRequest()

    function loadRankCategoryOptionsData(): void {
        loadRankCategoryOptionsRequest.run(
            () => rankCategoryService.getAllPages(),
            categories => setRankCategoryOptions(categories)
        )
    }

    // The categories changed: the table and the options
    function reloadRankCategoryData(): void {
        rankCategoryList.reload()
        loadRankCategoryOptionsData()
    }

    useOnMount(() => loadRankCategoryOptionsData())

    const rankRows = rankList.items.map((r) => (
        <Table.Tr key={r.id}>
            <Table.Td>{r.name}</Table.Td>
            <Table.Td>{r.order}</Table.Td>
            <Table.Td>{r.category.name}</Table.Td>
            <Table.Td>
                <ActionButton label="Ver" icon={<EyeIcon/>} onClick={() => {
                    setRank(r)
                    openViewRankModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Editar" icon={<PencilIcon/>} onClick={() => {
                    rankForm.setValues({
                        id: r.id,
                        name: r.name,
                        order: r.order,
                        categoryId: r.category.id
                    })
                    openCreateUpdateRankModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setRankToBeDeleted(r)
                    openDeleteRankModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    ))

    const rankCategoryRows = rankCategoryList.items.map((rc) => (
        <Table.Tr key={rc.id}>
            <Table.Td>{rc.name}</Table.Td>
            <Table.Td>
                <ActionButton label="Ver" icon={<EyeIcon/>} onClick={() => {
                    setRankCategory(rc)
                    openViewRankCategoryModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Editar" icon={<PencilIcon/>} onClick={() => {
                    rankCategoryForm.setValues({
                        id: rc.id,
                        name: rc.name
                    })
                    openCreateUpdateRankCategoryModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setRankCategoryToBeDeleted(rc)
                    openDeleteRankCategoryModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    ))

    return (
        <Tabs defaultValue="ranks">
            <Tabs.List>
                <Tabs.Tab
                    data-testid="rank-tab"
                    value="ranks"
                >Graus</Tabs.Tab>
                <Tabs.Tab
                    data-testid="rank-category-tab"
                    value="rankCategories"
                >Categorias</Tabs.Tab>
            </Tabs.List>

            <Tabs.Panel value="ranks" pt="md">
                <Button
                    data-testid="new-rank-button"
                    variant="filled"
                    onClick={() => {
                        rankForm.reset()
                        openCreateUpdateRankModal()
                    }}
                >Novo</Button>

                <Select
                    label="Categoria"
                    data={[
                        {
                            value: ALL_RANK_CATEGORIES,
                            label: ALL_RANK_CATEGORIES
                        },
                        ...rankCategoryOptions.map(r => ({
                            value: r.id,
                            label: r.name
                        }))
                    ]}
                    value={rankList.filter ?? ALL_RANK_CATEGORIES}
                    allowDeselect={false}
                    onChange={(value) => rankList.changeFilter(value === ALL_RANK_CATEGORIES ? null : value)}
                />
                <RequestErrorAlert error={loadRankCategoryOptionsRequest.error} onClose={loadRankCategoryOptionsRequest.clearError}/>

                <ViewObjectModal
                    dataTestId="rank-view-modal"
                    isViewObjectModalOpen={isViewRankModalOpen}
                    closeViewObjectModal={closeViewRankModal}
                    labelValue={rank ? convertRankToView(rank) : []}
                />

                <Modal
                    opened={isCreateUpdateRankModalOpen}
                    onClose={() => {
                        closeCreateUpdateRankModal()
                        saveRankRequest.clearError()
                    }}
                    title="Grau"
                    centered
                >
                    <form
                        data-testid="rank-form"
                        onSubmit={rankForm.onSubmit((values) => saveRankRequest.run(async () => {
                            if (!values.id) {
                                await rankService.create(values)
                            } else {
                                await rankService.updateById(values.id, values)
                            }

                            closeCreateUpdateRankModal()
                            showSuccessNotification("Grau salvo")
                            rankList.reload()
                        }))}
                    >
                        <RequestErrorAlert error={saveRankRequest.error} onClose={saveRankRequest.clearError}/>
                        <TextInput
                            withAsterisk
                            label="Nome"
                            placeholder="Faixa Branca"
                            key={rankForm.key('name')}
                            {...rankForm.getInputProps('name')}
                        />
                        <NumberInput
                            withAsterisk
                            label="Ordem"
                            placeholder="1"
                            key={rankForm.key('order')}
                            disabled={!!rankForm.values.id}
                            {...rankForm.getInputProps('order')}
                        />
                        <Select
                            withAsterisk
                            label="Categoria"
                            placeholder="Adulto"
                            data={
                                rankCategoryOptions.map(rc => ({
                                    value: rc.id,
                                    label: rc.name
                                }))
                            }
                            key={rankForm.key('categoryId')}
                            disabled={!!rankForm.values.id}
                            {...rankForm.getInputProps('categoryId')}
                        />
                        <Button
                            data-testid="save-rank-button"
                            variant="filled"
                            type="submit"
                            loading={saveRankRequest.isLoading}
                        >Salvar</Button>
                    </form>
                </Modal>

                <DeleteConfirmModal
                    opened={isDeleteRankModalOpen}
                    onClose={closeDeleteRankModal}
                    title="Deletar grau?"
                    confirmButtonTestId="delete-rank-button"
                    isLoading={deleteRankRequest.isLoading}
                    error={deleteRankRequest.error}
                    onCloseError={deleteRankRequest.clearError}
                    onConfirm={() => deleteRankRequest.run(async () => {
                        if (!rankToBeDeleted) {
                            throw new Error("rankToBeDeleted is empty")
                        }

                        await rankService.deleteById(rankToBeDeleted.id)
                        setRankToBeDeleted(null)
                        closeDeleteRankModal()
                        showSuccessNotification("Grau deletado")
                        rankList.reload()
                    })}
                >
                    Tem certeza que deseja deletar o grau {rankToBeDeleted?.name}?
                </DeleteConfirmModal>

                <RequestErrorAlert error={rankList.error} onClose={rankList.clearError}/>
                <RequestLoader isLoading={rankList.isLoading} hasLoaded={rankList.hasLoaded}>
                    <Table striped mt="md">
                        <Table.Thead>
                            <Table.Tr>
                                <Table.Th>Nome</Table.Th>
                                <Table.Th>Ordem</Table.Th>
                                <Table.Th>Categoria</Table.Th>
                                <Table.Th></Table.Th>
                                <Table.Th></Table.Th>
                                <Table.Th></Table.Th>
                            </Table.Tr>
                        </Table.Thead>
                        <Table.Tbody
                            data-testid="rank-body-table"
                        >{rankRows.length > 0 ? rankRows : <EmptyTableRow colSpan={6}/>}</Table.Tbody>
                    </Table>
                </RequestLoader>

                <ListPagination list={rankList}/>
            </Tabs.Panel>

            <Tabs.Panel value="rankCategories" pt="md">
                <Button
                    data-testid="new-rank-category-button"
                    variant="filled"
                    onClick={() => {
                        rankCategoryForm.reset()
                        openCreateUpdateRankCategoryModal()
                    }}
                >Novo</Button>

                <ViewObjectModal
                    dataTestId="rank-category-view-modal"
                    isViewObjectModalOpen={isViewRankCategoryModalOpen}
                    closeViewObjectModal={closeViewRankCategoryModal}
                    labelValue={rankCategory ? convertRankCategoryToView(rankCategory) : []}
                />

                <Modal
                    opened={isCreateUpdateRankCategoryModalOpen}
                    onClose={() => {
                        closeCreateUpdateRankCategoryModal()
                        saveRankCategoryRequest.clearError()
                    }}
                    title="Categoria"
                    centered
                >
                    <form
                        data-testid="rank-category-form"
                        onSubmit={rankCategoryForm.onSubmit((values) => saveRankCategoryRequest.run(async () => {
                            if (!values.id) {
                                await rankCategoryService.create(values)
                            } else {
                                await rankCategoryService.updateById(values.id, values)
                            }

                            closeCreateUpdateRankCategoryModal()
                            showSuccessNotification("Categoria salva")
                            reloadRankCategoryData()
                        }))}
                    >
                        <RequestErrorAlert error={saveRankCategoryRequest.error} onClose={saveRankCategoryRequest.clearError}/>
                        <TextInput
                            withAsterisk
                            label="Nome"
                            placeholder="Adulto"
                            key={rankCategoryForm.key('name')}
                            {...rankCategoryForm.getInputProps('name')}
                        />
                        <Button
                            variant="filled"
                            type="submit"
                            loading={saveRankCategoryRequest.isLoading}
                        >Salvar</Button>
                    </form>
                </Modal>

                <DeleteConfirmModal
                    opened={isDeleteRankCategoryModalOpen}
                    onClose={closeDeleteRankCategoryModal}
                    title="Deletar categoria?"
                    confirmButtonTestId="delete-rank-category-button"
                    isLoading={deleteRankCategoryRequest.isLoading}
                    error={deleteRankCategoryRequest.error}
                    onCloseError={deleteRankCategoryRequest.clearError}
                    onConfirm={() => deleteRankCategoryRequest.run(async () => {
                        if (!rankCategoryToBeDeleted) {
                            throw new Error("rankCategoryToBeDeleted is empty")
                        }

                        await rankCategoryService.deleteById(rankCategoryToBeDeleted.id)
                        setRankCategoryToBeDeleted(null)
                        closeDeleteRankCategoryModal()
                        showSuccessNotification("Categoria deletada")
                        reloadRankCategoryData()
                    })}
                >
                    Tem certeza que deseja deletar a categoria {rankCategoryToBeDeleted?.name}?
                </DeleteConfirmModal>

                <RequestErrorAlert error={rankCategoryList.error} onClose={rankCategoryList.clearError}/>
                <RequestLoader isLoading={rankCategoryList.isLoading} hasLoaded={rankCategoryList.hasLoaded}>
                    <Table striped mt="md">
                        <Table.Thead>
                            <Table.Tr>
                                <Table.Th>Nome</Table.Th>
                                <Table.Th></Table.Th>
                                <Table.Th></Table.Th>
                                <Table.Th></Table.Th>
                            </Table.Tr>
                        </Table.Thead>
                        <Table.Tbody
                            data-testid="rank-category-body-table"
                        >{rankCategoryRows.length > 0 ? rankCategoryRows : <EmptyTableRow colSpan={4}/>}</Table.Tbody>
                    </Table>
                </RequestLoader>

                <ListPagination list={rankCategoryList}/>
            </Tabs.Panel>
        </Tabs>
    )
}

interface RankForm {
    id: string
    name: string
    order: number
    categoryId: string
}

interface RankCategoryForm {
    id: string
    name: string
}
