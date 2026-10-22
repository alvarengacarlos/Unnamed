package com.alvarengacarlos.unnamed.app.gradingcandidate;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import com.alvarengacarlos.unnamed.app.common.PaymentMethod;

public record GradingCandidateInput() {
    public static record Create(
        @NotNull PaymentMethod paymentMethod,
        @NotNull Boolean isPaid,
        @NotNull UUID memberId,
        @NotNull UUID rankId,
        @NotNull UUID gradingId
    ) {}

    public static record Update(
        @NotNull PaymentMethod paymentMethod,
        @NotNull Boolean isPaid,
        @NotNull UUID rankId
    ) {}

    public static record GetGradingCandidates(
        Optional<UUID> gradingId) {
        public GetGradingCandidates {
            if (gradingId == null) {
                gradingId = Optional.empty();
            }
        }
    }
}
