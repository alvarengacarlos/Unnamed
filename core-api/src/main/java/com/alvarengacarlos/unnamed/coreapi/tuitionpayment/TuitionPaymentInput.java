package com.alvarengacarlos.unnamed.coreapi.tuitionpayment;

import java.time.Month;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record TuitionPaymentInput() {
    public static record Create(
        @NotNull Month month,
        @NotNull Short year,
        @NotNull Boolean isPaid,
        @NotNull UUID studentEnrollmentId) {
    }

    public static record Update(
        @NotNull Month month,
        @NotNull Short year,
        @NotNull Boolean isPaid) {
    }

    public static record GetTuitionPayments(
        Optional<UUID> studentId) {
        public GetTuitionPayments {
            if (studentId == null) {
                studentId = Optional.empty();
            }
        }
    }
}
