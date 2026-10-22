import { convertDateTimeToView, convertLocalDateTimeToView, convertToLabelValues } from "../common/common.converter"
import type { LabelValue } from "../common/common.type"
import { TournamentMatchResult, type TournamentMatch } from "./tournament-match.type"

export function convertTournamentMatchToView(tournamentMatch: TournamentMatch): Array<LabelValue> {
    return convertToLabelValues({
        "Id": tournamentMatch.id,
        "Torneio": tournamentMatch.tournament.name,
        "Competidor A": tournamentMatch.competitorA.member.name,
        "Competidor B": tournamentMatch.competitorB.member.name,
        "Data": convertLocalDateTimeToView(tournamentMatch.when),
        "Local": tournamentMatch.where,
        "Resultado": convertTournamentMatchResultOfMatchToView(tournamentMatch),
        "Criado em": convertDateTimeToView(tournamentMatch.createdAt),
        "Atualizado em": convertDateTimeToView(tournamentMatch.updatedAt),
        "Organização": tournamentMatch.organizationId
    })
}

// With the name of the winner, as Vitória de João Silva
export function convertTournamentMatchResultOfMatchToView(tournamentMatch: TournamentMatch): string {
    switch (tournamentMatch.result) {
        case TournamentMatchResult.A:
            return `Vitória de ${tournamentMatch.competitorA.member.name}`
        case TournamentMatchResult.B:
            return `Vitória de ${tournamentMatch.competitorB.member.name}`
        default:
            return convertTournamentMatchResultToView(tournamentMatch.result)
    }
}

export function convertTournamentMatchResultToView(result: TournamentMatchResult): string {
    switch (result) {
        case TournamentMatchResult.PENDING:
            return "Pendente"
        case TournamentMatchResult.CANCELLED:
            return "Cancelada"
        case TournamentMatchResult.NO_CONTEST:
            return "Sem resultado"
        case TournamentMatchResult.A:
            return "Vitória do competidor A"
        case TournamentMatchResult.B:
            return "Vitória do competidor B"
        case TournamentMatchResult.DRAW:
            return "Empate"
        default:
            throw new Error("TournamentMatchResult does not exist")
    }
}
