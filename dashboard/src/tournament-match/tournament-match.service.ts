import { CrudService, RequestClient } from "../common/common.service"
import { TournamentMatch, TournamentMatchResult, type TournamentMatchOutput } from "./tournament-match.type"

export class TournamentMatchService extends CrudService<TournamentMatch, TournamentMatchOutput, CreateTournamentMatchInput, UpdateTournamentMatchInput, GetAllTournamentMatchesFilterInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/tournament-matches", TournamentMatch.from)
    }

    // All the matches or none: the API creates them in one transaction
    async createInBulk(bulk: Array<CreateTournamentMatchInput>): Promise<Array<TournamentMatch>> {
        console.info(`Creating ${this.path}/bulk`)
        const outputs = await this.requestClient.post<{ bulk: Array<CreateTournamentMatchInput> }, Array<TournamentMatchOutput>>(
            `${this.path}/bulk`,
            { bulk }
        )
        console.info("Success")
        return outputs.map(this.from)
    }
}

interface CreateTournamentMatchInput {
    readonly when: string
    readonly where: string
    readonly tournamentCompetitorAId: string
    readonly tournamentCompetitorBId: string
    readonly tournamentId: string
}

interface UpdateTournamentMatchInput {
    readonly when: string
    readonly where: string
    readonly result: TournamentMatchResult
}

interface GetAllTournamentMatchesFilterInput {
    readonly tournamentId?: string
}
