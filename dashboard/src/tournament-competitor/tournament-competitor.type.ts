import { PaymentMethod } from "../common/common.type";
import { Member, type MemberOutput } from "../member/member.type";
import { Tournament, type TournamentOutput } from "../tournament/tournament.type";

export class TournamentCompetitor {
    constructor(
        public readonly id: string,
        public readonly paymentMethod: PaymentMethod,
        public readonly isPaid: boolean,
        public readonly points: number,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly tournament: Tournament,
        public readonly member: Member,
        public readonly organizationId: string
    ) {}

    static from(tournamentCompetitor: TournamentCompetitorOutput): TournamentCompetitor {
        return new TournamentCompetitor(
            tournamentCompetitor.id,
            tournamentCompetitor.paymentMethod,
            tournamentCompetitor.isPaid,
            tournamentCompetitor.points,
            new Date(tournamentCompetitor.createdAt),
            new Date(tournamentCompetitor.updatedAt),
            Tournament.from(tournamentCompetitor.tournament),
            Member.from(tournamentCompetitor.member),
            tournamentCompetitor.organizationId
        )
    }
}

export interface TournamentCompetitorOutput {
    readonly id: string
    readonly paymentMethod: PaymentMethod
    readonly isPaid: boolean
    readonly points: number
    readonly createdAt: string
    readonly updatedAt: string
    readonly tournament: TournamentOutput
    readonly member: MemberOutput
    readonly organizationId: string
}
