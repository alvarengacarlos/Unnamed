import { Paper, SimpleGrid, Text, Title } from "@mantine/core";
import { useState } from "react";
import { memberService, studentService, teacherService } from "../instance";
import { useLoadRequest, useOnMount } from "../common/hook/useRequest.hook";
import RequestErrorAlert from "../common/component/RequestErrorAlert.component";
import RequestLoader from "../common/component/RequestLoader.component";

export default function Dashboard() {
    const [totals, setTotals] = useState<Array<Total>>([])

    const loadTotalsRequest = useLoadRequest()

    function loadTotalsData() {
        // Only the total of each listing is used, so the first page (0) with 1 record is enough
        loadTotalsRequest.run(
            () => Promise.all([
                memberService.getAll(0, 1),
                studentService.getAll(0, 1),
                teacherService.getAll(0, 1)
            ]),
            ([members, students, teachers]) => setTotals([
                { label: "Membros", total: members.totalElements },
                { label: "Alunos", total: students.totalElements },
                { label: "Professores", total: teachers.totalElements }
            ])
        )
    }

    useOnMount(() => loadTotalsData())

    return (
        <>
            <Title order={2}>Dashboard</Title>

            <RequestErrorAlert error={loadTotalsRequest.error} onClose={loadTotalsRequest.clearError}/>
            <RequestLoader isLoading={loadTotalsRequest.isLoading} hasLoaded={loadTotalsRequest.hasLoaded}>
                <SimpleGrid cols={{ base: 1, sm: 3 }} mt="md">
                    {totals.map(t => (
                        <Paper key={t.label} withBorder p="md" radius="md">
                            <Text size="sm" c="dimmed">{t.label}</Text>
                            <Text fz={32} fw={700}>{t.total.toLocaleString("pt-BR")}</Text>
                        </Paper>
                    ))}
                </SimpleGrid>
            </RequestLoader>
        </>
    )
}

interface Total {
    label: string
    total: number
}
