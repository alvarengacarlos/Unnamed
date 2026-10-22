import { Rank, type RankOutput } from "../rank/rank.type";

export class Grading {
    constructor(
        public readonly id: string,
        public readonly name: string,
        // Date and time without time zone, as 2026-10-21T10:00:00
        public readonly when: string,
        public readonly where: string,
        // Dates as yyyy-mm-dd
        public readonly registrationPeriodStart: string,
        public readonly registrationPeriodEnd: string,
        public readonly gradingPrices: Array<GradingPrice>,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly organizationId: string
    ) {}

    static from(grading: GradingOutput): Grading {
        return new Grading(
            grading.id,
            grading.name,
            grading.when,
            grading.where,
            grading.registrationPeriodStart,
            grading.registrationPeriodEnd,
            grading.gradingPrices.map(GradingPrice.from),
            new Date(grading.createdAt),
            new Date(grading.updatedAt),
            grading.organizationId
        )
    }
}

export interface GradingOutput {
    readonly id: string
    readonly name: string
    readonly when: string
    readonly where: string
    readonly registrationPeriodStart: string
    readonly registrationPeriodEnd: string
    readonly gradingPrices: Array<GradingPriceOutput>
    readonly createdAt: string
    readonly updatedAt: string
    readonly organizationId: string
}

// The price of the grading for a candidate who aims at the rank
export class GradingPrice {
    constructor(
        public readonly price: number,
        public readonly rank: Rank
    ) {}

    static from(gradingPrice: GradingPriceOutput): GradingPrice {
        return new GradingPrice(gradingPrice.price, Rank.from(gradingPrice.rank))
    }
}

export interface GradingPriceOutput {
    readonly price: number
    readonly rank: RankOutput
}
