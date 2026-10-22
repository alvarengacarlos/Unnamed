import { Button, Group, Modal, NumberInput, Select, Table, TextInput } from "@mantine/core"
import { DateInput } from "@mantine/dates";
import { useDisclosure } from "@mantine/hooks";
import z from "zod/v4";
import { requiredText } from "../common/common.schema";
import { schemaResolver, useForm } from "@mantine/form";
import { useState } from "react";
import { PencilIcon, TrashIcon, EyeIcon, MagnifyingGlassIcon } from "@phosphor-icons/react";
import { Gender, type Member } from "./member.type"
import { memberService, rankService } from "../instance";
import type { Rank } from "../rank/rank.type";
import { cpfRegex } from "../common/common.type";
import ViewObjectModal from "../common/component/ViewObjectModal.component";
import { booleanOptions, convertBooleanToView, convertSearchToMemberFilter } from "../common/common.converter";
import { convertGenderToView, convertMemberToView } from "./member.converter";
import { useLoadRequest, useOnMount, useRequest } from "../common/hook/useRequest.hook";
import { usePaginatedList } from "../common/hook/usePaginatedList.hook";
import { showSuccessNotification } from "../common/common.notification";
import RequestErrorAlert from "../common/component/RequestErrorAlert.component";
import RequestLoader from "../common/component/RequestLoader.component";
import ActionButton from "../common/component/ActionButton.component";
import DeleteConfirmModal from "../common/component/DeleteConfirmModal.component";
import EmptyTableRow from "../common/component/EmptyTableRow.component";
import ListPagination from "../common/component/ListPagination.component";
import MaskedTextInput from "../common/component/MaskedTextInput.component";

const memberFormSchema = z.object({
    name: requiredText('Nome é obrigatório'),
    phone: z.string({ error: 'Telefone é obrigatório'}).regex(/^\([0-9]{2}\) [0-9]{4,5}-[0-9]{4}$/, {error: 'Telefone incompleto'}),
    cpf: z.string({error: 'CPF é obrigatório'}).regex(cpfRegex, {error: 'CPF incompleto'}),
    address: requiredText('Endereço é obrigatório'),
    gender: z.enum(Gender, { error: 'Sexo é obrigatório'}),
    weight: z.number({error: 'Peso é obrigatório'}).positive({error: 'Peso deve ser positivo'}),
    height: z.number({error: 'Altura é obrigatório'}).positive({error: 'Altura deve ser positiva'}),
    // The DateInput gives the date as yyyy-mm-dd, the format of the API
    birthDate: z.string({error: 'Data de nascimento é obrigatório'}).regex(/^\d{4}-\d{2}-\d{2}$/, {error: 'Data de nascimento é obrigatório'}),
    isPwd: z.boolean({error: 'PCD é obrigatório'}),
    isCompetitor: z.boolean({error: 'Competidor é obrigatório'}),
    rankId: z.uuid({error: 'Grau é obrigatório'})
})

const genderOptions = [Gender.MALE, Gender.FEMALE].map(gender => ({
    value: gender,
    label: convertGenderToView(gender)
}))

export default function MemberTab() {
    const [isViewMemberModalOpen, { open: openViewMemberModal, close: closeViewMemberModal }] = useDisclosure(false)
    const [isCreateUpdateMemberModalOpen, { open: openCreateUpdateMemberModal, close: closeCreateUpdateMemberModal }] = useDisclosure(false)
    const [isDeleteMemberModalOpen, { open: openDeleteMemberModal, close: closeDeleteMemberModal }] = useDisclosure(false)

    const memberForm = useForm<MemberForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(memberFormSchema)
    })
    const [memberToBeDeleted, setMemberToBeDeleted] = useState<Member | null>()
    const [member, setMember] = useState<Member>()
    const [ranks, setRanks] = useState<Array<Rank>>([])

    const memberList = usePaginatedList((page, pageSize, search) => memberService.getAll(page, pageSize, convertSearchToMemberFilter(search)))
    const loadRankRequest = useLoadRequest()
    const saveMemberRequest = useRequest()
    const deleteMemberRequest = useRequest()

    // The ranks are the options of the member form
    function loadRankData() {
        loadRankRequest.run(
            () => rankService.getAllPages(),
            ranks => setRanks(ranks)
        )
    }

    useOnMount(() => loadRankData())

    const memberRows = memberList.items.map(m =>
        <Table.Tr key={m.id}>
            <Table.Td>{m.name}</Table.Td>
            <Table.Td>{m.cpf}</Table.Td>
            <Table.Td>{m.rank.name}</Table.Td>
            <Table.Td>{convertBooleanToView(m.isCompetitor)}</Table.Td>
            <Table.Td>
                <ActionButton label="Ver" icon={<EyeIcon/>} onClick={() => {
                    setMember(m)
                    openViewMemberModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Editar" icon={<PencilIcon/>} onClick={() => {
                    memberForm.setValues({
                        id: m.id,
                        name: m.name,
                        phone: m.phone,
                        cpf: m.cpf,
                        address: m.address,
                        gender: m.gender,
                        weight: m.weight,
                        height: m.height,
                        birthDate: m.birthDate,
                        isPwd: m.isPwd,
                        isCompetitor: m.isCompetitor,
                        rankId: m.rank.id,
                    })
                    openCreateUpdateMemberModal()
                }}/>
            </Table.Td>
            <Table.Td>
                <ActionButton label="Deletar" icon={<TrashIcon/>} onClick={() => {
                    setMemberToBeDeleted(m)
                    openDeleteMemberModal()
                }}/>
            </Table.Td>
        </Table.Tr>
    )

    return (
        <>
            <Group justify="space-between">
                <Button
                    data-testid="new-member-button"
                    variant="filled"
                    onClick={() => {
                        memberForm.reset()
                        openCreateUpdateMemberModal()
                    }}
                >Novo</Button>
                <TextInput
                    aria-label="Buscar"
                    placeholder="Buscar por nome ou CPF"
                    leftSection={<MagnifyingGlassIcon/>}
                    value={memberList.filter}
                    onChange={(event) => memberList.changeSearch(event.currentTarget.value)}
                />
            </Group>

            <ViewObjectModal
                dataTestId="member-view-modal"
                isViewObjectModalOpen={isViewMemberModalOpen}
                closeViewObjectModal={closeViewMemberModal}
                labelValue={ member ? convertMemberToView(member) : []}
            />

            <Modal
                opened={isCreateUpdateMemberModalOpen}
                onClose={() => {
                    closeCreateUpdateMemberModal()
                    saveMemberRequest.clearError()
                }}
                title="Membro"
                centered
            >
                <form
                    data-testid="member-form"
                    onSubmit={memberForm.onSubmit((values) => saveMemberRequest.run(async () => {
                        if (!values.id) {
                            await memberService.create(values)
                        } else {
                            await memberService.updateById(values.id, values)
                        }

                        closeCreateUpdateMemberModal()
                        showSuccessNotification("Membro salvo")
                        memberList.reload()
                    }))}
                >
                    <RequestErrorAlert error={loadRankRequest.error} onClose={loadRankRequest.clearError}/>
                    <RequestErrorAlert error={saveMemberRequest.error} onClose={saveMemberRequest.clearError}/>
                    <TextInput
                        withAsterisk
                        label="Nome"
                        placeholder="João"
                        key={memberForm.key('name')}
                        {...memberForm.getInputProps('name')}
                    />
                    <MaskedTextInput
                        withAsterisk
                        label="CPF"
                        placeholder="000.000.000-00"
                        mask="000.000.000-00"
                        key={memberForm.key('cpf')}
                        {...memberForm.getInputProps('cpf')}
                    />
                    <MaskedTextInput
                        withAsterisk
                        label="Telefone"
                        placeholder="(00) 00000-0000"
                        // A landline has 8 digits and a cell phone 9, after the area code
                        mask={[{ mask: "(00) 0000-0000" }, { mask: "(00) 00000-0000" }]}
                        key={memberForm.key('phone')}
                        {...memberForm.getInputProps('phone')}
                    />
                    <TextInput
                        withAsterisk
                        label="Endereço"
                        placeholder="Rua A, Bairro B, Número 1"
                        key={memberForm.key('address')}
                        {...memberForm.getInputProps('address')}
                    />
                    <Select
                        withAsterisk
                        label="Sexo"
                        placeholder="Masculino"
                        data={genderOptions}
                        key={memberForm.key('gender')}
                        {...memberForm.getInputProps('gender')}
                    />
                    <NumberInput
                        withAsterisk
                        label="Peso"
                        placeholder="72,5 kg"
                        suffix=" kg"
                        decimalSeparator=","
                        decimalScale={2}
                        key={memberForm.key('weight')}
                        {...memberForm.getInputProps('weight')}
                    />
                    <NumberInput
                        withAsterisk
                        label="Altura"
                        placeholder="1,80 m"
                        suffix=" m"
                        decimalSeparator=","
                        decimalScale={2}
                        key={memberForm.key('height')}
                        {...memberForm.getInputProps('height')}
                    />
                    <DateInput
                        withAsterisk
                        label="Data de nascimento"
                        placeholder="dd/mm/aaaa"
                        valueFormat="DD/MM/YYYY"
                        maxDate={new Date()}
                        key={memberForm.key('birthDate')}
                        {...memberForm.getInputProps('birthDate')}
                    />
                    <Select
                        withAsterisk
                        label="PCD"
                        placeholder="Não"
                        data={booleanOptions}
                        key={memberForm.key('isPwd')}
                        {...memberForm.getInputProps('isPwd')}
                    />
                    <Select
                        withAsterisk
                        label="Competidor"
                        placeholder="Não"
                        data={booleanOptions}
                        key={memberForm.key('isCompetitor')}
                        {...memberForm.getInputProps('isCompetitor')}
                    />
                    <Select
                        withAsterisk
                        label="Grau"
                        placeholder="Faixa Branca - Adulto"
                        data={
                            ranks.map(r => ({
                                value: r.id,
                                label: `${r.name} - ${r.category.name}`
                            }))
                        }
                        key={memberForm.key('rankId')}
                        {...memberForm.getInputProps('rankId')}
                    />
                    <Button variant="filled" type="submit" loading={saveMemberRequest.isLoading}>Salvar</Button>
                </form>
            </Modal>

            <DeleteConfirmModal
                opened={isDeleteMemberModalOpen}
                onClose={closeDeleteMemberModal}
                title="Deletar membro?"
                isLoading={deleteMemberRequest.isLoading}
                error={deleteMemberRequest.error}
                onCloseError={deleteMemberRequest.clearError}
                onConfirm={() => deleteMemberRequest.run(async () => {
                    if (!memberToBeDeleted) {
                        throw new Error("memberToBeDeleted is empty")
                    }

                    await memberService.deleteById(memberToBeDeleted.id)
                    setMemberToBeDeleted(null)
                    closeDeleteMemberModal()
                    showSuccessNotification("Membro deletado")
                    memberList.reload()
                })}
            >
                Tem certeza que deseja deletar o membro {memberToBeDeleted?.name}?
            </DeleteConfirmModal>

            <RequestErrorAlert error={memberList.error} onClose={memberList.clearError}/>
            <RequestLoader isLoading={memberList.isLoading} hasLoaded={memberList.hasLoaded}>
                <Table striped mt="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Nome</Table.Th>
                            <Table.Th>CPF</Table.Th>
                            <Table.Th>Grau</Table.Th>
                            <Table.Th>Competidor</Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                            <Table.Th></Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody data-testid="member-body-table">
                        {memberRows.length > 0 ? memberRows : <EmptyTableRow colSpan={7}/>}
                    </Table.Tbody>
                </Table>
            </RequestLoader>

            <ListPagination list={memberList}/>
        </>
    )
}

interface MemberForm {
    id: string
    name: string
    cpf: string
    phone: string
    address: string
    gender: Gender
    weight: number
    height: number
    birthDate: string
    isPwd: boolean
    isCompetitor: boolean
    rankId: string
}
