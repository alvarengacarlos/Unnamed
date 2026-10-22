import { DayOfWeek } from "../common/common.type"
import { Teacher, type TeacherOutput } from "../member/member.type";

export class Clazz {
    constructor(
        public readonly id: string,
        public readonly name: string,
        public readonly martialArt: MartialArt,
        public readonly startTime: string,
        public readonly endTime: string,
        public readonly weekdays: Array<DayOfWeek>,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly teacher: Teacher,
        public readonly organizationId: string
    ) {}

    static from(clazz: ClazzOutput): Clazz {
        return new Clazz(
            clazz.id,
            clazz.name,
            clazz.martialArt,
            clazz.startTime,
            clazz.endTime,
            clazz.weekdays,
            new Date(clazz.createdAt),
            new Date(clazz.updatedAt),
            Teacher.from(clazz.teacher),
            clazz.organizationId
        )
    }
}

export interface ClazzOutput {
    readonly id: string
    readonly name: string
    readonly martialArt: MartialArt
    readonly startTime: string
    readonly endTime: string
    // The API answers a set of days as a JSON array, without repetitions
    readonly weekdays: Array<DayOfWeek>
    readonly createdAt: string
    readonly updatedAt: string
    readonly teacher: TeacherOutput
    readonly organizationId: string
}

export enum MartialArt {
    JIU_JITSU = "JIU_JITSU",
    JUDO = "JUDO",
    KARATE = "KARATE",
    MUAY_THAI = "MUAY_THAI",
    BOXING = "BOXING",
    TAEKWONDO = "TAEKWONDO",
    MMA = "MMA",
    KICKBOXING = "KICKBOXING",
    KUNG_FU = "KUNG_FU"
}
