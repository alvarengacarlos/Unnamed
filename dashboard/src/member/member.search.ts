import type { ComboboxItem } from "@mantine/core"
import { memberService, studentService } from "../instance"

// The searches of the SearchableCpfSelect: the record with the CPF, as an option with its name

export async function searchMemberOptionsByCpf(cpf: string): Promise<Array<ComboboxItem>> {
    const paginationOfMembers = await memberService.getAll(0, 1, { cpf })
    return paginationOfMembers.content.map(m => ({ label: m.name, value: m.id }))
}

export async function searchStudentOptionsByCpf(cpf: string): Promise<Array<ComboboxItem>> {
    const paginationOfStudents = await studentService.getAll(0, 1, { cpf })
    return paginationOfStudents.content.map(s => ({ label: s.member.name, value: s.id }))
}
