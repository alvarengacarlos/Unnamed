import { teacherService } from "../instance";
import { convertTeacherToView } from "./member.converter";
import MemberRoleTab from "./MemberRoleTab";

export default function TeacherTab() {
    return (
        <MemberRoleTab
            service={teacherService}
            title="Professor"
            convertToView={convertTeacherToView}
            viewTestId="teacher-view-modal"
        />
    )
}
