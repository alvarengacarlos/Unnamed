import { CrudService, RequestClient } from "../common/common.service"
import { PaymentMethod } from "../common/common.type"
import { GradingCandidate, type GradingCandidateOutput } from "./grading-candidate.type"

export class GradingCandidateService extends CrudService<GradingCandidate, GradingCandidateOutput, CreateGradingCandidateInput, UpdateGradingCandidateInput, GetAllGradingCandidatesFilterInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/grading-candidates", GradingCandidate.from)
    }
}

interface CreateGradingCandidateInput {
    readonly paymentMethod: PaymentMethod
    readonly isPaid: boolean
    readonly memberId: string
    readonly rankId: string
    readonly gradingId: string
}

interface UpdateGradingCandidateInput {
    readonly paymentMethod: PaymentMethod
    readonly isPaid: boolean
    readonly rankId: string
}

interface GetAllGradingCandidatesFilterInput {
    readonly gradingId?: string
}
