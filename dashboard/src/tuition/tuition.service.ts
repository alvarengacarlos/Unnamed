import { CrudService, RequestClient } from "../common/common.service"
import { Tuition, type TuitionOutput } from "./tuition.type"

export class TuitionService extends CrudService<Tuition, TuitionOutput, CreateUpdateTuitionInput, CreateUpdateTuitionInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/tuitions", Tuition.from)
    }
}

interface CreateUpdateTuitionInput {
    readonly name: string
    readonly price: number
}
