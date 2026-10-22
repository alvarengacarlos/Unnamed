import { CrudService, RequestClient } from "../common/common.service"
import { StudentEnrollment, type StudentEnrollmentOutput } from "./student-enrollment.type"

export class StudentEnrollmentService extends CrudService<StudentEnrollment, StudentEnrollmentOutput, CreateStudentEnrollmentInput, UpdateStudentEnrollmentInput, GetAllStudentEnrollmentsFilterInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/student-enrollments", StudentEnrollment.from)
    }
}

interface CreateStudentEnrollmentInput {
    readonly tuitionId: string
    readonly studentId: string
    readonly clazzId: string
}

interface GetAllStudentEnrollmentsFilterInput {
    readonly studentId?: string
    readonly clazzId?: string
}

interface UpdateStudentEnrollmentInput {
    readonly isActive: boolean
    readonly tuitionId: string
}
