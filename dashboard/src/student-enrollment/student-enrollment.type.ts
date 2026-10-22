import { Clazz, type ClazzOutput } from "../clazz/clazz.type";
import { Student, type StudentOutput } from "../member/member.type";
import { Tuition, type TuitionOutput } from "../tuition/tuition.type";

export class StudentEnrollment {
    constructor(
        public readonly id: string,
        public readonly isActive: boolean,
        public readonly tuition: Tuition,
        public readonly student: Student,
        public readonly clazz: Clazz,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly organizationId: string
    ) {}

    static from(studentEnrollment: StudentEnrollmentOutput): StudentEnrollment {
        return new StudentEnrollment(
            studentEnrollment.id,
            studentEnrollment.isActive,
            Tuition.from(studentEnrollment.tuition),
            Student.from(studentEnrollment.student),
            Clazz.from(studentEnrollment.clazz),
            new Date(studentEnrollment.createdAt),
            new Date(studentEnrollment.updatedAt),
            studentEnrollment.organizationId
        )
    }
}

export interface StudentEnrollmentOutput {
    readonly id: string
    readonly isActive: boolean
    readonly tuition: TuitionOutput
    readonly student: StudentOutput
    readonly clazz: ClazzOutput
    readonly createdAt: string
    readonly updatedAt: string
    readonly organizationId: string
}
