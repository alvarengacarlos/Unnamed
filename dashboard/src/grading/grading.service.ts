import { CrudService, RequestClient } from "../common/common.service"
import { Grading, type GradingOutput } from "./grading.type"

export class GradingService extends CrudService<Grading, GradingOutput, CreateUpdateGradingInput, CreateUpdateGradingInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/gradings", Grading.from)
    }
}

interface CreateUpdateGradingInput {
    readonly name: string
    readonly when: string
    readonly where: string
    readonly registrationPeriodStart: string
    readonly registrationPeriodEnd: string
    // The price by the id of the rank
    readonly prices: Record<string, number>
}
