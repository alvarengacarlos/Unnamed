import { PaymentMethod } from "../common/common.type";
import { Grading, type GradingOutput } from "../grading/grading.type";
import { Member, type MemberOutput } from "../member/member.type";
import { Rank, type RankOutput } from "../rank/rank.type";

export class GradingCandidate {
    constructor(
        public readonly id: string,
        public readonly paymentMethod: PaymentMethod,
        public readonly isPaid: boolean,
        public readonly createdAt: Date,
        public readonly updatedAt: Date,
        // The rank the candidate aims at
        public readonly rank: Rank,
        public readonly grading: Grading,
        public readonly member: Member,
        public readonly organizationId: string
    ) {}

    static from(gradingCandidate: GradingCandidateOutput): GradingCandidate {
        return new GradingCandidate(
            gradingCandidate.id,
            gradingCandidate.paymentMethod,
            gradingCandidate.isPaid,
            new Date(gradingCandidate.createdAt),
            new Date(gradingCandidate.updatedAt),
            Rank.from(gradingCandidate.rank),
            Grading.from(gradingCandidate.grading),
            Member.from(gradingCandidate.member),
            gradingCandidate.organizationId
        )
    }
}

export interface GradingCandidateOutput {
    readonly id: string
    readonly paymentMethod: PaymentMethod
    readonly isPaid: boolean
    readonly createdAt: string
    readonly updatedAt: string
    readonly rank: RankOutput
    readonly grading: GradingOutput
    readonly member: MemberOutput
    readonly organizationId: string
}
