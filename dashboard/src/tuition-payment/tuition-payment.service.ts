import { CrudService, RequestClient } from "../common/common.service"
import { TuitionPayment, type TuitionPaymentOutput } from "./tuition-payment.type"

export class TuitionPaymentService extends CrudService<TuitionPayment, TuitionPaymentOutput, CreateTuitionPaymentInput, UpdateTuitionPaymentInput, GetAllTuitionPaymentsFilterInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/tuition-payments", TuitionPayment.from)
    }
}

interface CreateTuitionPaymentInput {
    // The number of the month, January being 1
    readonly month: number
    readonly year: number
    readonly isPaid: boolean
    readonly studentEnrollmentId: string
}

interface UpdateTuitionPaymentInput {
    // The number of the month, January being 1
    readonly month: number
    readonly year: number
    readonly isPaid: boolean
}

interface GetAllTuitionPaymentsFilterInput {
    readonly studentId?: string
}
