import { Modal, Tabs } from "@mantine/core"
import type { Tournament } from "./tournament.type";
import TournamentCompetitorTab from "../tournament-competitor/TournamentCompetitorTab";
import TournamentMatchTab from "../tournament-match/TournamentMatchTab";

interface Props {
    opened: boolean
    onClose: () => void
    // The tournament whose competitors and matches are managed
    tournament?: Tournament
}

export default function TournamentCompetitionModal(props: Props) {
    return (
        <Modal opened={props.opened} onClose={props.onClose} title={`Competidores e lutas - ${props.tournament?.name ?? ""}`} fullScreen>
            {/* The content mounts each time the modal opens, so it starts clean for the tournament chosen */}
            {props.tournament && (
                // Not kept mounted, so the matches load again the competitors added in the other tab
                <Tabs defaultValue="tournamentCompetitors" keepMounted={false}>
                    <Tabs.List>
                        <Tabs.Tab value="tournamentCompetitors">Competidores</Tabs.Tab>
                        <Tabs.Tab value="tournamentMatches">Lutas</Tabs.Tab>
                    </Tabs.List>

                    <Tabs.Panel value="tournamentCompetitors" pt="md">
                        <TournamentCompetitorTab tournament={props.tournament}/>
                    </Tabs.Panel>

                    <Tabs.Panel value="tournamentMatches" pt="md">
                        <TournamentMatchTab tournament={props.tournament}/>
                    </Tabs.Panel>
                </Tabs>
            )}
        </Modal>
    )
}
