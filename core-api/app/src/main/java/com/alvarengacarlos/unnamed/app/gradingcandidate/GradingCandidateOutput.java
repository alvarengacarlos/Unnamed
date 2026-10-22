package com.alvarengacarlos.unnamed.app.gradingcandidate;

import java.time.Instant;
import java.util.UUID;

import com.alvarengacarlos.unnamed.app.common.PaymentMethod;
import com.alvarengacarlos.unnamed.app.grading.GradingOutput;
import com.alvarengacarlos.unnamed.app.member.MemberOutput;
import com.alvarengacarlos.unnamed.app.rank.RankOutput;

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
