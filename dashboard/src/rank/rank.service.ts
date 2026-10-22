import { CrudService, RequestClient } from "../common/common.service"
import { Rank, RankCategory, type RankCategoryOutput, type RankOutput } from "./rank.type"

export class RankService extends CrudService<Rank, RankOutput, CreateRankInput, UpdateRankInput, GetAllRanksFilterInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/ranks", Rank.from)
    }
}

interface CreateRankInput {
    readonly name: string
    readonly order: number
    readonly categoryId: string
}

interface UpdateRankInput {
    readonly name: string
}

interface GetAllRanksFilterInput {
    readonly categoryId?: string
}

export class RankCategoryService extends CrudService<RankCategory, RankCategoryOutput, CreateUpdateRankCategoryInput, CreateUpdateRankCategoryInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/rank-categories", RankCategory.from)
    }
}

interface CreateUpdateRankCategoryInput {
    readonly name: string
}
