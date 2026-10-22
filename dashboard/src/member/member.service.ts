import { Gender, Member, Student, Teacher, type MemberOutput, type StudentOutput, type TeacherOutput } from "./member.type"
import { CrudService, ResourceService, RequestClient } from "../common/common.service"

export class MemberService extends CrudService<Member, MemberOutput, CreateUpdateMemberInput, CreateUpdateMemberInput, GetAllMembersFilterInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/members", Member.from)
    }
}

interface CreateUpdateMemberInput {
    readonly name: string
    readonly phone: string
    readonly cpf: string
    readonly address: string
    readonly gender: Gender
    readonly weight: number
    readonly height: number
    readonly birthDate: string
    readonly isPwd: boolean
    readonly isCompetitor: boolean
    readonly rankId: string
}

// The name matches when it contains the text, ignoring the case; the CPF matches only when equal
interface SearchMemberFilterInput {
    readonly name?: string
    readonly cpf?: string
}

type GetAllMembersFilterInput = SearchMemberFilterInput

export class StudentService extends ResourceService<Student, StudentOutput, CreateStudentInput, GetAllStudentsFilterInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/students", Student.from)
    }
}

interface CreateStudentInput {
    readonly memberId: string
}

interface GetAllStudentsFilterInput extends SearchMemberFilterInput {
    readonly memberId?: string
}

export class TeacherService extends ResourceService<Teacher, TeacherOutput, CreateTeacherInput, SearchMemberFilterInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/teachers", Teacher.from)
    }
}

interface CreateTeacherInput {
    readonly memberId: string
}
