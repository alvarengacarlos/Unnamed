import { TournamentCompetitor, type TournamentCompetitorOutput } from "../tournament-competitor/tournament-competitor.type";
import { Tournament, type TournamentOutput } from "../tournament/tournament.type";

export class TournamentMatch {
    constructor(
        public readonly id: string,
        // Date and time without time zone, as 2026-10-21T10:00:00
        public readonly when: string,
        public readonly where: string,
        public readonly result: TournamentMatchResult,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly competitorA: TournamentCompetitor,
        public readonly competitorB: TournamentCompetitor,
        public readonly tournament: Tournament,
        public readonly organizationId: string
    ) {}

    static from(tournamentMatch: TournamentMatchOutput): TournamentMatch {
        return new TournamentMatch(
            tournamentMatch.id,
            tournamentMatch.when,
            tournamentMatch.where,
            tournamentMatch.result,
            new Date(tournamentMatch.createdAt),
            new Date(tournamentMatch.updatedAt),
            TournamentCompetitor.from(tournamentMatch.competitorA),
            TournamentCompetitor.from(tournamentMatch.competitorB),
            Tournament.from(tournamentMatch.tournament),
            tournamentMatch.organizationId
        )
    }
}

export interface TournamentMatchOutput {
    readonly id: string
    readonly when: string
    readonly where: string
    readonly result: TournamentMatchResult
    readonly createdAt: string
    readonly updatedAt: string
    readonly competitorA: TournamentCompetitorOutput
    readonly competitorB: TournamentCompetitorOutput
    readonly tournament: TournamentOutput
    readonly organizationId: string
}

export enum TournamentMatchResult {
    PENDING = "PENDING",
    CANCELLED = "CANCELLED",
    NO_CONTEST = "NO_CONTEST",
    A = "A",
    B = "B",
    DRAW = "DRAW"
}
