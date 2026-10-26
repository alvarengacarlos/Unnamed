package com.alvarengacarlos.unnamed.coreapi.gradingcandidate;

import java.time.Instant;
import java.util.UUID;

import com.alvarengacarlos.unnamed.coreapi.common.PaymentMethod;
import com.alvarengacarlos.unnamed.coreapi.grading.GradingOutput;
import com.alvarengacarlos.unnamed.coreapi.member.MemberOutput;
import com.alvarengacarlos.unnamed.coreapi.rank.RankOutput;

public record GradingCandidateOutput(
    UUID id,
    PaymentMethod paymentMethod,
    Boolean isPaid,
    Instant createdAt,
    Instant updatedAt,
    RankOutput rank,
    GradingOutput grading,
    MemberOutput member,
    UUID organizationId) {
    public static GradingCandidateOutput from(GradingCandidate gradingCandidate) {
        return new GradingCandidateOutput(
            gradingCandidate.getId(),
            gradingCandidate.getPaymentMethod(),
            gradingCandidate.getIsPaid(),
            gradingCandidate.getCreatedAt(),
            gradingCandidate.getUpdatedAt(),
            RankOutput.from(gradingCandidate.getRank()),
            GradingOutput.from(gradingCandidate.getGrading()),
            MemberOutput.from(gradingCandidate.getMember()),
            gradingCandidate.getOrganizationId()
        );
    }
}
