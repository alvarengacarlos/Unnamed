import { convertBooleanToView, convertDateTimeToView, convertLocalDateTimeToView, convertPriceToView, convertToLabelValues } from "../common/common.converter"
import type { LabelValue } from "../common/common.type"
import { convertGenderToView, convertHeightToView, convertWeightToView } from "../member/member.converter"
import { TournamentFormat, type Tournament } from "./tournament.type"

export function convertTournamentToView(tournament: Tournament): Array<LabelValue> {
    return convertToLabelValues({
        "Id": tournament.id,
        "Nome": tournament.name,
        "Formato": convertTournamentFormatToView(tournament.format),
        "Critérios de divisão": convertDivisionCriterionToView(tournament),
        "Preço de participação": convertPriceToView(tournament.participationPrice),
        "Início": convertLocalDateTimeToView(tournament.startDateTime),
        "Fim": convertLocalDateTimeToView(tournament.endDateTime),
        "Local": tournament.where,
        "Criado em": convertDateTimeToView(tournament.createdAt),
        "Atualizado em": convertDateTimeToView(tournament.updatedAt),
        "Organização": tournament.organizationId
    })
}

export function convertDivisionCriterionToView(tournament: Tournament): string {
    const divisionCriterion = tournament.divisionCriterion
    const criteria: Array<string> = []
    if (divisionCriterion.gender) {
        criteria.push(`Sexo: ${convertGenderToView(divisionCriterion.gender)}`)
    }
    if (divisionCriterion.weight !== undefined) {
        criteria.push(`Peso: ${convertWeightToView(divisionCriterion.weight)}`)
    }
    if (divisionCriterion.height !== undefined) {
        criteria.push(`Altura: ${convertHeightToView(divisionCriterion.height)}`)
    }
    if (divisionCriterion.age !== undefined) {
        criteria.push(`Idade: ${divisionCriterion.age} anos`)
    }
    if (divisionCriterion.isPwd !== undefined) {
        criteria.push(`PCD: ${convertBooleanToView(divisionCriterion.isPwd)}`)
    }
    if (divisionCriterion.rank) {
        criteria.push(`Grau: ${divisionCriterion.rank.name} - ${divisionCriterion.rank.category.name}`)
    }

    return criteria.length > 0 ? criteria.join("; ") : "Nenhum"
}

export function convertTournamentFormatToView(format: TournamentFormat): string {
    switch (format) {
        case TournamentFormat.TITLE_MATCH:
            return "Disputa de título"
        case TournamentFormat.SINGLE_ELIMINATION:
            return "Eliminatória simples"
        case TournamentFormat.ROUND_ROBIN:
            return "Todos contra todos"
        default:
            throw new Error("TournamentFormat does not exist")
    }
}
