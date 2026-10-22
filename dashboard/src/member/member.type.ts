import { Rank, type RankOutput } from "../rank/rank.type";

export class Member {
    constructor(
        public readonly id: string,
        public readonly name: string,
        public readonly phone: string,
        public readonly cpf: string,
        public readonly address: string,
        public readonly gender: Gender,
        public readonly weight: number,
        public readonly height: number,
        public readonly birthDate: string,
        public readonly isPwd: boolean,
        public readonly isCompetitor: boolean,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly rank: Rank,
        public readonly organizationId: string
    ) {}

    static from(member: MemberOutput): Member {
        return new Member(
            member.id,
            member.name,
            member.phone,
            member.cpf,
            member.address,
            member.gender,
            member.weight,
            member.height,
            member.birthDate,
            member.isPwd,
            member.isCompetitor,
            new Date(member.createdAt),
            new Date(member.updatedAt),
            Rank.from(member.rank),
            member.organizationId
        )
    }
}

export interface MemberOutput {
    readonly id: string
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
    readonly createdAt: string
    readonly updatedAt: string
    readonly rank: RankOutput
    readonly organizationId: string
}

export  enum Gender {
    MALE = "MALE",
    FEMALE = "FEMALE"
}

export class Student {
    constructor(
        public readonly id: string,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly member: Member,
        public readonly organizationId: string
    ) {}

    static from(student: StudentOutput): Student {
        return new Student(
            student.id,
            new Date(student.createdAt),
            new Date(student.updatedAt),
            Member.from(student.member),
            student.organizationId
        )
    }
}

export interface StudentOutput {
    readonly id: string
    readonly createdAt: string
    readonly updatedAt: string
    readonly member: MemberOutput
    readonly organizationId: string
}

export class Teacher {
    constructor(
        public readonly id: string,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly member: Member,
        public readonly organizationId: string
    ) {}

    static from(teacher: TeacherOutput): Teacher {
        return new Teacher(
            teacher.id,
            new Date(teacher.createdAt),
            new Date(teacher.updatedAt),
            Member.from(teacher.member),
            teacher.organizationId
        )
    }
}

export interface TeacherOutput {
    readonly id: string
    readonly createdAt: string
    readonly updatedAt: string
    readonly member: MemberOutput
    readonly organizationId: string
}
