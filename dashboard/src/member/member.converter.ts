import { convertBooleanToView, convertDateTimeToView, convertDateToView, convertToLabelValues } from "../common/common.converter"
import type { LabelValue } from "../common/common.type"
import { Gender, type Member, type Student, type Teacher } from "./member.type"

export function convertMemberToView(member: Member): Array<LabelValue> {
    return convertToLabelValues({
        "Id": member.id,
        ...buildMemberStudentTeacherCommonProperties(member),
        "Criado em": convertDateTimeToView(member.createdAt),
        "Atualizado em": convertDateTimeToView(member.updatedAt),
        "Organização": member.organizationId
    })
}

export function convertStudentToView(student: Student): Array<LabelValue> {
    return convertToLabelValues({
        "Id": student.id,
        ...buildMemberStudentTeacherCommonProperties(student.member),
        "Criado em": convertDateTimeToView(student.createdAt),
        "Atualizado em": convertDateTimeToView(student.updatedAt),
        "Organização": student.organizationId
    })
}

export function convertTeacherToView(teacher: Teacher): Array<LabelValue> {
    return convertToLabelValues({
        "Id": teacher.id,
        ...buildMemberStudentTeacherCommonProperties(teacher.member),
        "Criado em": convertDateTimeToView(teacher.createdAt),
        "Atualizado em": convertDateTimeToView(teacher.updatedAt),
        "Organização": teacher.organizationId
    })
}

function buildMemberStudentTeacherCommonProperties(member: Member): Record<string, string> {
    return {
        "Nome": member.name,
        "CPF": member.cpf,
        "Telefone": member.phone,
        "Endereço": member.address,
        "Sexo": convertGenderToView(member.gender),
        "Peso": convertWeightToView(member.weight),
        "Altura": convertHeightToView(member.height),
        "Data de nascimento": convertDateToView(member.birthDate),
        "PCD": convertBooleanToView(member.isPwd),
        "Competidor": convertBooleanToView(member.isCompetitor),
        "Grau": `${member.rank.name} - ${member.rank.category.name}`,
    }
}

const weightFormat = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 })
const heightFormat = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })

// As 72,5 kg
export function convertWeightToView(weight: number): string {
    return `${weightFormat.format(weight)} kg`
}

// As 1,80 m
export function convertHeightToView(height: number): string {
    return `${heightFormat.format(height)} m`
}

export function convertGenderToView(gender: Gender): string {
    switch (gender) {
        case Gender.MALE:
            return "Masculino"
        case Gender.FEMALE:
            return "Feminino"
        default:
            throw new Error("Gender does not exist")
    }
}
