import { Tabs } from "@mantine/core"
import MemberTab from "./MemberTab";
import StudentTab from "./StudentTab";
import TeacherTab from "./TeacherTab";

export default function Member() {
    return (
        <Tabs defaultValue="members">
            <Tabs.List>
                <Tabs.Tab data-testid="members-tab" value="members">Membros</Tabs.Tab>
                <Tabs.Tab data-testid="students-tab" value="students">Alunos</Tabs.Tab>
                <Tabs.Tab data-testid="teachers-tab" value="teachers">Professores</Tabs.Tab>
            </Tabs.List>

            <Tabs.Panel value="members" pt="md">
                <MemberTab/>
            </Tabs.Panel>

            <Tabs.Panel value="students" pt="md">
                <StudentTab/>
            </Tabs.Panel>

            <Tabs.Panel value="teachers" pt="md">
                <TeacherTab/>
            </Tabs.Panel>
        </Tabs>
    )
}
