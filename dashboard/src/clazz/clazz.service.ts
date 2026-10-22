import { Clazz, MartialArt, type ClazzOutput } from "./clazz.type"
import { DayOfWeek } from "../common/common.type"
import { CrudService, RequestClient } from "../common/common.service"

export class ClazzService extends CrudService<Clazz, ClazzOutput, CreateUpdateClazzInput, CreateUpdateClazzInput, GetAllClazzesFilterInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/clazzes", Clazz.from)
    }
}

interface CreateUpdateClazzInput {
    readonly name: string
    readonly martialArt: MartialArt
    readonly startTime: string
    readonly endTime: string
    readonly weekdays: Array<DayOfWeek>
    readonly teacherId: string
}

interface GetAllClazzesFilterInput {
    readonly martialArt?: MartialArt
}
