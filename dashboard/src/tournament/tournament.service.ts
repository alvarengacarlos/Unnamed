import { CrudService, RequestClient } from "../common/common.service"
import { Gender, Member, type MemberOutput } from "../member/member.type"
import { Tournament, TournamentFormat, type TournamentOutput } from "./tournament.type"

export class TournamentService extends CrudService<Tournament, TournamentOutput, CreateUpdateTournamentInput, CreateUpdateTournamentInput> {
    constructor(requestClient: RequestClient) {
        super(requestClient, "/tournaments", Tournament.from)
    }

    // The members who meet the division criteria of the tournament. The differences widen the weight, the height and the age of the division criteria, such as 5 kg above or below
    async getPossibleCompetitors(id: string, input: GetPossibleCompetitorsInput): Promise<Array<Member>> {
        console.info(`Getting ${this.path}/${id}/possible-competitors`)
        const queryParams = new Map<string, string>()
        for (const [name, value] of Object.entries(input)) {
            if (value !== undefined) {
                queryParams.set(name, String(value))
            }
        }

        const members = await this.requestClient.get<Array<MemberOutput>>(`${this.path}/${id}/possible-competitors`, queryParams)
        console.info("Success")
        return members.map(Member.from)
    }
}

interface CreateUpdateTournamentInput {
    readonly name: string
    readonly format: TournamentFormat
    // The API names the criteria in upper case; a criterion left null does not divide the competitors
    readonly divisionCriterion: {
        readonly GENDER: Gender | null
        readonly WEIGHT: number | null
        readonly HEIGHT: number | null
        readonly AGE: number | null
        readonly IS_PWD: boolean | null
        readonly RANK: string | null
    }
    readonly participationPrice: number
    readonly startDateTime: string
    readonly endDateTime: string
    readonly where: string
}

interface GetPossibleCompetitorsInput {
    readonly weightDifference?: number
    readonly heightDifference?: number
    readonly ageDifference?: number
}
