import { Gender } from "../member/member.type";
import { Rank, type RankOutput } from "../rank/rank.type";

export class Tournament {
    constructor(
        public readonly id: string,
        public readonly name: string,
        public readonly format: TournamentFormat,
        public readonly divisionCriterion: DivisionCriterion,
        public readonly participationPrice: number,
        // Dates and times without time zone, as 2026-10-21T10:00:00
        public readonly startDateTime: string,
        public readonly endDateTime: string,
        public readonly where: string,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly organizationId: string
    ) {}

    static from(tournament: TournamentOutput): Tournament {
        const divisionCriterion = tournament.divisionCriterion
        return new Tournament(
            tournament.id,
            tournament.name,
            tournament.format,
            {
                gender: divisionCriterion.GENDER,
                weight: divisionCriterion.WEIGHT,
                height: divisionCriterion.HEIGHT,
                age: divisionCriterion.AGE,
                isPwd: divisionCriterion.IS_PWD,
                rank: divisionCriterion.RANK ? Rank.from(divisionCriterion.RANK) : undefined
            },
            tournament.participationPrice,
            tournament.startDateTime,
            tournament.endDateTime,
            tournament.where,
            new Date(tournament.createdAt),
            new Date(tournament.updatedAt),
            tournament.organizationId
        )
    }
}

// What the competitors of the tournament have in common. A criterion left out does not divide them.
export interface DivisionCriterion {
    readonly gender?: Gender
    readonly weight?: number
    readonly height?: number
    readonly age?: number
    readonly isPwd?: boolean
    readonly rank?: Rank
}

export interface TournamentOutput {
    readonly id: string
    readonly name: string
    readonly format: TournamentFormat
    // Only the criteria the tournament has, by their names in the API
    readonly divisionCriterion: {
        readonly GENDER?: Gender
        readonly WEIGHT?: number
        readonly HEIGHT?: number
        readonly AGE?: number
        readonly IS_PWD?: boolean
        readonly RANK?: RankOutput
    }
    readonly participationPrice: number
    readonly startDateTime: string
    readonly endDateTime: string
    readonly where: string
    readonly createdAt: string
    readonly updatedAt: string
    readonly organizationId: string
}

export enum TournamentFormat {
    TITLE_MATCH = "TITLE_MATCH",
    SINGLE_ELIMINATION = "SINGLE_ELIMINATION",
    ROUND_ROBIN = "ROUND_ROBIN"
}
