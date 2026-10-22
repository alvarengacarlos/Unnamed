export class Tuition {
    constructor(
        public readonly id: string,
        public readonly name: string,
        public readonly price: number,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        public readonly organizationId: string
    ) {}

    static from(tuition: TuitionOutput): Tuition {
        return new Tuition(
            tuition.id,
            tuition.name,
            tuition.price,
            new Date(tuition.createdAt),
            new Date(tuition.updatedAt),
            tuition.organizationId
        )
    }
}

export interface TuitionOutput {
    readonly id: string
    readonly name: string
    readonly price: number
    readonly createdAt: string
    readonly updatedAt: string
    readonly organizationId: string
}
