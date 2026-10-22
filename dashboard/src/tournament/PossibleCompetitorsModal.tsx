import { Button, Checkbox, Group, Modal, NumberInput, Select, Table, Text } from "@mantine/core";
import { schemaResolver, useForm } from "@mantine/form";
import { useState } from "react";
import z from "zod/v4";
import { tournamentCompetitorService, tournamentService } from "../instance";
import type { Tournament } from "./tournament.type";
import type { Member } from "../member/member.type";
import { PaymentMethod } from "../common/common.type";
import { booleanOptions, convertBooleanToView, convertDateToView, convertPaymentMethodToView, paymentMethodOptions } from "../common/common.converter";
import { convertDivisionCriterionToView } from "./tournament.converter";
import { convertHeightToView, convertWeightToView } from "../member/member.converter";
import { useLoadRequest, useOnMount, useRequest } from "../common/hook/useRequest.hook";
import { showSuccessNotification } from "../common/common.notification";
import RequestErrorAlert from "../common/component/RequestErrorAlert.component";
import RequestLoader from "../common/component/RequestLoader.component";
import EmptyTableRow from "../common/component/EmptyTableRow.component";

const enrollFormSchema = z.object({
    paymentMethod: z.enum(PaymentMethod, { error: 'Forma de pagamento é obrigatório' }),
    isPaid: z.boolean({ error: 'Pago é obrigatório' })
})

interface Props {
    opened: boolean
    onClose: () => void
    tournament?: Tournament
}

// The members who meet the division criteria of a tournament, to enroll the ones chosen as competitors
export default function PossibleCompetitorsModal(props: Props) {
    return (
        <Modal opened={props.opened} onClose={props.onClose} title={`Possíveis competidores - ${props.tournament?.name ?? ""}`} fullScreen>
            {/* The content mounts each time the modal opens, so it searches the members of the tournament chosen */}
            {props.tournament && <PossibleCompetitorsContent tournament={props.tournament}/>}
        </Modal>
    )
}

interface ContentProps {
    tournament: Tournament
}

function PossibleCompetitorsContent(props: ContentProps) {
    const divisionCriterion = props.tournament.divisionCriterion

    // An empty NumberInput gives '': no difference widens that criterion
    const [weightDifference, setWeightDifference] = useState<number | string>('')
    const [heightDifference, setHeightDifference] = useState<number | string>('')
    const [ageDifference, setAgeDifference] = useState<number | string>('')

    const [members, setMembers] = useState<Array<Member>>([])
    const [selectedMemberIds, setSelectedMemberIds] = useState<Array<string>>([])

    const enrollForm = useForm<EnrollForm>({
        mode: 'uncontrolled',
        validate: schemaResolver(enrollFormSchema)
    })

    const searchRequest = useLoadRequest()
    const enrollRequest = useRequest()

    function search() {
        searchRequest.run(
            () => tournamentService.getPossibleCompetitors(props.tournament.id, {
                weightDifference: typeof weightDifference === 'number' ? weightDifference : undefined,
                heightDifference: typeof heightDifference === 'number' ? heightDifference : undefined,
                ageDifference: typeof ageDifference === 'number' ? ageDifference : undefined
            }),
            possibleCompetitors => {
                setMembers(possibleCompetitors)
                setSelectedMemberIds([])
            }
        )
    }

    useOnMount(search)

    const areAllSelected = members.length > 0 && selectedMemberIds.length === members.length

    const memberRows = members.map(m => (
        <Table.Tr key={m.id}>
            <Table.Td>
                <Checkbox
                    aria-label={`Selecionar ${m.name}`}
                    checked={selectedMemberIds.includes(m.id)}
                    onChange={(event) => setSelectedMemberIds(event.currentTarget.checked
                        ? [...selectedMemberIds, m.id]
                        : selectedMemberIds.filter(id => id !== m.id))}
                />
            </Table.Td>
            <Table.Td>{m.name}</Table.Td>
            <Table.Td>{m.cpf}</Table.Td>
            <Table.Td>{convertDateToView(m.birthDate)}</Table.Td>
            <Table.Td>{convertWeightToView(m.weight)}</Table.Td>
            <Table.Td>{convertHeightToView(m.height)}</Table.Td>
            <Table.Td>{`${m.rank.name} - ${m.rank.category.name}`}</Table.Td>
            <Table.Td>{convertBooleanToView(m.isPwd)}</Table.Td>
        </Table.Tr>
    ))

    return (
        <>
            <Text size="sm" c="dimmed">Critérios de divisão: {convertDivisionCriterionToView(props.tournament)}</Text>

            <Group align="flex-end" mt="sm">
                <NumberInput
                    label="Diferença de peso"
                    description="Para mais ou para menos"
                    placeholder="5 kg"
                    suffix=" kg"
                    decimalSeparator=","
                    decimalScale={2}
                    min={0}
                    // Only the criteria of the tournament can be widened
                    disabled={divisionCriterion.weight === undefined}
                    value={weightDifference}
                    onChange={setWeightDifference}
                />
                <NumberInput
                    label="Diferença de altura"
                    description="Para mais ou para menos"
                    placeholder="0,10 m"
                    suffix=" m"
                    decimalSeparator=","
                    decimalScale={2}
                    min={0}
                    disabled={divisionCriterion.height === undefined}
                    value={heightDifference}
                    onChange={setHeightDifference}
                />
                <NumberInput
                    label="Diferença de idade"
                    description="Para mais ou para menos"
                    placeholder="2 anos"
                    suffix=" anos"
                    allowDecimal={false}
                    min={0}
                    disabled={divisionCriterion.age === undefined}
                    value={ageDifference}
                    onChange={setAgeDifference}
                />
                <Button variant="light" loading={searchRequest.isLoading} onClick={search}>Buscar</Button>
            </Group>

            <RequestErrorAlert error={searchRequest.error} onClose={searchRequest.clearError}/>
            <RequestLoader isLoading={searchRequest.isLoading} hasLoaded={searchRequest.hasLoaded}>
                <Table striped mt="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>
                                <Checkbox
                                    aria-label="Selecionar todos"
                                    checked={areAllSelected}
                                    indeterminate={selectedMemberIds.length > 0 && !areAllSelected}
                                    onChange={() => setSelectedMemberIds(areAllSelected ? [] : members.map(m => m.id))}
                                />
                            </Table.Th>
                            <Table.Th>Nome</Table.Th>
                            <Table.Th>CPF</Table.Th>
                            <Table.Th>Nascimento</Table.Th>
                            <Table.Th>Peso</Table.Th>
                            <Table.Th>Altura</Table.Th>
                            <Table.Th>Grau</Table.Th>
                            <Table.Th>PCD</Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>{memberRows.length > 0 ? memberRows : <EmptyTableRow colSpan={8}/>}</Table.Tbody>
                </Table>
            </RequestLoader>

            <form onSubmit={enrollForm.onSubmit((values) => enrollRequest.run(async () => {
                const competitors = await tournamentCompetitorService.createInBulk(selectedMemberIds.map(memberId => ({
                    ...values,
                    memberId,
                    tournamentId: props.tournament.id
                })))

                setSelectedMemberIds([])
                showSuccessNotification(`${competitors.length} competidor(es) inscrito(s)`)
            }))}>
                <RequestErrorAlert error={enrollRequest.error} onClose={enrollRequest.clearError}/>
                <Group align="flex-end" mt="md">
                    <Select
                        withAsterisk
                        label="Forma de pagamento"
                        placeholder={convertPaymentMethodToView(PaymentMethod.PIX)}
                        data={paymentMethodOptions}
                        key={enrollForm.key('paymentMethod')}
                        {...enrollForm.getInputProps('paymentMethod')}
                    />
                    <Select
                        withAsterisk
                        label="Pago"
                        placeholder="Não"
                        data={booleanOptions}
                        key={enrollForm.key('isPaid')}
                        {...enrollForm.getInputProps('isPaid')}
                    />
                    <Button
                        type="submit"
                        disabled={selectedMemberIds.length === 0}
                        loading={enrollRequest.isLoading}
                    >Inscrever selecionados ({selectedMemberIds.length})</Button>
                </Group>
            </form>
        </>
    )
}

interface EnrollForm {
    paymentMethod: PaymentMethod
    isPaid: boolean
}
