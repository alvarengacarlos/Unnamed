import { CrudService, RequestClient } from "../common/common.service"
import { PaymentMethod } from "../common/common.type"
import { TournamentCompetitor, type TournamentCompetitorOutput } from "./tournament-competitor.type"

export class TournamentCompetitorService extends CrudService<TournamentCompetitor, TournamentCompetitorOutput, CreateTournamentCompetitorInput, UpdateTournamentCompetitorInput, GetAllTournamentCompetitorsFilterInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/tournament-competitors", TournamentCompetitor.from)
    }

    // All the competitors or none: the API creates them in one transaction
    async createInBulk(bulk: Array<CreateTournamentCompetitorInput>): Promise<Array<TournamentCompetitor>> {
        console.info(`Creating ${this.path}/bulk`)
        const outputs = await this.requestClient.post<{ bulk: Array<CreateTournamentCompetitorInput> }, Array<TournamentCompetitorOutput>>(
            `${this.path}/bulk`,
            { bulk }
        )
        console.info("Success")
        return outputs.map(this.from)
    }
}

interface CreateTournamentCompetitorInput {
    readonly paymentMethod: PaymentMethod
    readonly isPaid: boolean
    readonly tournamentId: string
    readonly memberId: string
}

interface UpdateTournamentCompetitorInput {
    readonly paymentMethod: PaymentMethod
    readonly isPaid: boolean
}

interface GetAllTournamentCompetitorsFilterInput {
    readonly tournamentId?: string
}
