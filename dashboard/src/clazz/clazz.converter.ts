import { convertDateTimeToView, convertDayOfWeekToView, convertToLabelValues } from "../common/common.converter"
import type { LabelValue } from "../common/common.type"
import { MartialArt, type Clazz } from "./clazz.type"

export function convertClazzToView(clazz: Clazz): Array<LabelValue> {
    return convertToLabelValues({
        "Id": clazz.id,
        "Nome": clazz.name,
        "Arte marcial": convertMartialArtToView(clazz.martialArt),
        "Horário": convertScheduleToView(clazz),
        "Dias da semana": convertWeekdaysToView(clazz),
        "Criado em": convertDateTimeToView(clazz.createdAt),
        "Atualizado em": convertDateTimeToView(clazz.updatedAt),
        "Organização": clazz.organizationId
    })
}

// The API answers the times as hh:mm:ss, but the classes start and end at whole minutes
export function convertTimeToView(time: string): string {
    return time.slice(0, 5)
}

// As 08:00 - 09:00
export function convertScheduleToView(clazz: Clazz): string {
    return `${convertTimeToView(clazz.startTime)} - ${convertTimeToView(clazz.endTime)}`
}

export function convertWeekdaysToView(clazz: Clazz): string {
    return clazz.weekdays.map(w => convertDayOfWeekToView(w)).join(", ")
}

export function convertMartialArtToView(martialArt: MartialArt): string {
    switch (martialArt) {
        case MartialArt.JIU_JITSU:
            return "Jiu Jitsu"
        case MartialArt.JUDO:
            return "Judo"
        case MartialArt.KARATE:
            return "Karate"
        case MartialArt.MUAY_THAI:
            return "Muay Thai"
        case MartialArt.BOXING:
            return "Boxing"
        case MartialArt.TAEKWONDO:
            return "Taekwondo"
        case MartialArt.MMA:
            return "MMA"
        case MartialArt.KICKBOXING:
            return "Kickboxing"
        case MartialArt.KUNG_FU:
            return "Kung Fu"
        default:
            throw new Error("MartialArt does not exist")
    }
}
