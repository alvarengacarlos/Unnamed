export class Rank {
    constructor(
        public readonly id: string,
        public readonly name: string,
        public readonly order: number,
        public readonly category: RankCategory,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly organizationId: string
    ) {}

    static from(rank: RankOutput): Rank {
        return new Rank(
            rank.id,
            rank.name,
            rank.order,
            RankCategory.from(rank.category),
            new Date(rank.createdAt),
            new Date(rank.updatedAt),
            rank.organizationId
        )
    }
}

export interface RankOutput {
    readonly id: string
    readonly name: string
    readonly order: number
    readonly category: RankCategoryOutput
    readonly createdAt: string
    readonly updatedAt: string
    readonly organizationId: string
}

export class RankCategory {
    constructor(
        public readonly id: string,
        public readonly name: string,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly organizationId: string
    ) {}

    static from(rankCategory: RankCategoryOutput): RankCategory {
        return new RankCategory(
            rankCategory.id,
            rankCategory.name,
            new Date(rankCategory.createdAt),
            new Date(rankCategory.updatedAt),
            rankCategory.organizationId
        )
    }
}

export interface RankCategoryOutput {
    readonly id: string
    readonly name: string
    readonly createdAt: string
    readonly updatedAt: string
    readonly organizationId: string
}
