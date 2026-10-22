import { Button, Fieldset, Group, Modal, Select, Stack, Text, TextInput } from "@mantine/core";
import { DateTimePicker } from "@mantine/dates";
import { schemaResolver, useForm } from "@mantine/form";
import z from "zod/v4";
import { requiredText } from "../common/common.schema";
import { PlusIcon, TrashIcon } from "@phosphor-icons/react";
import { tournamentMatchService } from "../instance";
import type { Tournament } from "../tournament/tournament.type";
import type { TournamentCompetitor } from "../tournament-competitor/tournament-competitor.type";
import { convertLocalDateTimeToService } from "../common/common.converter";
import { useRequest } from "../common/hook/useRequest.hook";
import { showSuccessNotification } from "../common/common.notification";
import RequestErrorAlert from "../common/component/RequestErrorAlert.component";
import ActionButton from "../common/component/ActionButton.component";

const tournamentMatchBulkFormSchema = z.object({
    matches: z.array(z.object({
        tournamentCompetitorAId: z.uuid({ error: 'Competidor A é obrigatório' }),
        tournamentCompetitorBId: z.uuid({ error: 'Competidor B é obrigatório' }),
        when: z.string({ error: 'Data e hora são obrigatórios' }),
        where: requiredText('Local é obrigatório')
    }).refine(match => match.tournamentCompetitorAId !== match.tournamentCompetitorBId, {
        error: 'Os competidores precisam ser diferentes',
        path: ['tournamentCompetitorBId']
    })).min(1, { error: 'Informe ao menos 1 luta' })
})

const emptyMatch: TournamentMatchForm = {
    tournamentCompetitorAId: null,
    tournamentCompetitorBId: null,
    when: null,
    where: ''
}

interface Props {
    opened: boolean
    onClose: () => void
    // Called after the matches are created, to load them again
    onCreated: () => void
    // The tournament of the matches
    tournament: Tournament
    // The competitors of the tournament, the options of the form
    tournamentCompetitors: Array<TournamentCompetitor>
}

// Several matches of a tournament created at once
export default function TournamentMatchBulkModal(props: Props) {
    return (
        <Modal opened={props.opened} onClose={props.onClose} title={`Novas lutas em lote - ${props.tournament.name}`} size="xl" centered>
            {/* The content mounts each time the modal opens, so the form starts clean */}
            {props.opened && <TournamentMatchBulkContent {...props}/>}
        </Modal>
    )
}

function TournamentMatchBulkContent(props: Props) {
    // Controlled, so the list of matches renders again when a match is added or removed
    const tournamentMatchBulkForm = useForm<TournamentMatchBulkForm>({
        initialValues: {
            matches: [emptyMatch]
        },
        validate: schemaResolver(tournamentMatchBulkFormSchema)
    })

    const createTournamentMatchesRequest = useRequest()

    const tournamentCompetitorOptions = props.tournamentCompetitors.map(tc => ({
        value: tc.id,
        label: tc.member.name
    }))

    const matches = tournamentMatchBulkForm.values.matches

    return (
        <form onSubmit={tournamentMatchBulkForm.onSubmit((values) => createTournamentMatchesRequest.run(async () => {
            // The schema checked the fields, so none of them is empty here
            const tournamentMatches = await tournamentMatchService.createInBulk(values.matches.map(match => ({
                tournamentId: props.tournament.id,
                tournamentCompetitorAId: match.tournamentCompetitorAId as string,
                tournamentCompetitorBId: match.tournamentCompetitorBId as string,
                when: convertLocalDateTimeToService(match.when as string),
                where: match.where
            })))

            props.onClose()
            showSuccessNotification(`${tournamentMatches.length} luta(s) criada(s)`)
            props.onCreated()
        }))}>
            <RequestErrorAlert error={createTournamentMatchesRequest.error} onClose={createTournamentMatchesRequest.clearError}/>
            <Stack gap="sm">
                {matches.map((_match, index) => (
                    <Fieldset key={index} legend={`Luta ${index + 1}`}>
                        <Group grow align="flex-start">
                            <Select
                                withAsterisk
                                searchable
                                label="Competidor A"
                                placeholder="João Silva"
                                data={tournamentCompetitorOptions}
                                {...tournamentMatchBulkForm.getInputProps(`matches.${index}.tournamentCompetitorAId`)}
                            />
                            <Select
                                withAsterisk
                                searchable
                                label="Competidor B"
                                placeholder="Pedro Souza"
                                data={tournamentCompetitorOptions}
                                {...tournamentMatchBulkForm.getInputProps(`matches.${index}.tournamentCompetitorBId`)}
                            />
                        </Group>
                        <Group align="flex-end" mt="xs">
                            <DateTimePicker
                                flex={1}
                                withAsterisk
                                label="Data e hora"
                                placeholder="dd/mm/aaaa hh:mm"
                                valueFormat="DD/MM/YYYY HH:mm"
                                {...tournamentMatchBulkForm.getInputProps(`matches.${index}.when`)}
                            />
                            <TextInput
                                flex={1}
                                withAsterisk
                                label="Local"
                                placeholder="Tatame 1"
                                {...tournamentMatchBulkForm.getInputProps(`matches.${index}.where`)}
                            />
                            <ActionButton
                                label="Remover luta"
                                icon={<TrashIcon/>}
                                onClick={() => tournamentMatchBulkForm.removeListItem('matches', index)}
                            />
                        </Group>
                    </Fieldset>
                ))}
            </Stack>
            {typeof tournamentMatchBulkForm.errors.matches === 'string' && (
                <Text c="red" size="sm">{tournamentMatchBulkForm.errors.matches}</Text>
            )}

            <Group justify="space-between" mt="md">
                <Button
                    variant="light"
                    leftSection={<PlusIcon/>}
                    // The new match starts with the date and the place of the last one, as they usually repeat
                    onClick={() => tournamentMatchBulkForm.insertListItem('matches', {
                        ...emptyMatch,
                        when: matches.at(-1)?.when ?? null,
                        where: matches.at(-1)?.where ?? ''
                    })}
                >Adicionar luta</Button>
                <Button type="submit" loading={createTournamentMatchesRequest.isLoading}>
                    Salvar {matches.length} luta(s)
                </Button>
            </Group>
        </form>
    )
}

// The fields start empty, so the inputs of the controlled form start without a value
interface TournamentMatchBulkForm {
    matches: Array<TournamentMatchForm>
}

interface TournamentMatchForm {
    tournamentCompetitorAId: string | null
    tournamentCompetitorBId: string | null
    when: string | null
    where: string
}
