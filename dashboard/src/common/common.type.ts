export enum DayOfWeek {
    MONDAY = "MONDAY",
    TUESDAY = "TUESDAY",
    WEDNESDAY = "WEDNESDAY",
    THURSDAY = "THURSDAY",
    FRIDAY = "FRIDAY",
    SATURDAY = "SATURDAY",
    SUNDAY = "SUNDAY"
}

export enum PaymentMethod {
    CREDIT_CARD = "CREDIT_CARD",
    DEBIT_CARD = "DEBIT_CARD",
    PIX = "PIX",
    MONEY = "MONEY"
}

export enum Month {
    JANUARY = "JANUARY",
    FEBRUARY = "FEBRUARY",
    MARCH = "MARCH",
    APRIL = "APRIL",
    MAY = "MAY",
    JUNE = "JUNE",
    JULY = "JULY",
    AUGUST = "AUGUST",
    SEPTEMBER = "SEPTEMBER",
    OCTOBER = "OCTOBER",
    NOVEMBER = "NOVEMBER",
    DECEMBER = "DECEMBER"
}

export class Pagination<T> {
    constructor(
        public readonly content: Array<T>,
        public readonly totalPages: number,
        // The number of records of all the pages
        public readonly totalElements: number
    ) {}

    static from<O, T>(paginationOutput: PaginationOutput<O>, from: (output: O) => T): Pagination<T> {
        return new Pagination(
            paginationOutput.content.map(from),
            paginationOutput.page.totalPages,
            paginationOutput.page.totalElements
        )
    }
}

export interface PaginationOutput<O> {
    readonly content: Array<O>
    readonly page: {
        readonly number: number
        readonly size: number
        readonly totalElements: number
        readonly totalPages: number
    }
}

export enum RoutePath {
    DASHBOARD = '/',
    RANK = '/ranks',
    MEMBER = '/members',
    TUITION = '/tuitions',
    CLAZZ = '/clazzes',
    GRADING = '/gradings',
    TOURNAMENT = '/tournaments',
    ASSISTANT = '/assistant'
}

export interface LabelValue {
    label: string
    value: string
}

export const cpfRegex = /^\d{3}\.\d{3}\.\d{3}-\d{2}$/
