import { Tabs } from "@mantine/core"
import GradingTab from "./GradingTab";

export default function Grading() {
    return (
        <Tabs defaultValue="gradings">
            <Tabs.List>
                <Tabs.Tab value="gradings">Graduações</Tabs.Tab>
            </Tabs.List>

            <Tabs.Panel value="gradings" pt="md">
                <GradingTab/>
            </Tabs.Panel>
        </Tabs>
    )
}
