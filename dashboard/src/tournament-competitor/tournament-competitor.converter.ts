import { convertBooleanToView, convertDateTimeToView, convertPaymentMethodToView, convertToLabelValues } from "../common/common.converter"
import type { LabelValue } from "../common/common.type"
import type { TournamentCompetitor } from "./tournament-competitor.type"

export function convertTournamentCompetitorToView(tournamentCompetitor: TournamentCompetitor): Array<LabelValue> {
    return convertToLabelValues({
        "Id": tournamentCompetitor.id,
        "Membro": tournamentCompetitor.member.name,
        "CPF": tournamentCompetitor.member.cpf,
        "Torneio": tournamentCompetitor.tournament.name,
        "Pontos": String(tournamentCompetitor.points),
        "Forma de pagamento": convertPaymentMethodToView(tournamentCompetitor.paymentMethod),
        "Pago": convertBooleanToView(tournamentCompetitor.isPaid),
        "Criado em": convertDateTimeToView(tournamentCompetitor.createdAt),
        "Atualizado em": convertDateTimeToView(tournamentCompetitor.updatedAt),
        "Organização": tournamentCompetitor.organizationId
    })
}
