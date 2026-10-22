import { Tabs } from "@mantine/core"
import TournamentTab from "./TournamentTab";

export default function Tournament() {
    return (
        <Tabs defaultValue="tournaments">
            <Tabs.List>
                <Tabs.Tab value="tournaments">Torneios</Tabs.Tab>
            </Tabs.List>

            <Tabs.Panel value="tournaments" pt="md">
                <TournamentTab/>
            </Tabs.Panel>
        </Tabs>
    )
}
