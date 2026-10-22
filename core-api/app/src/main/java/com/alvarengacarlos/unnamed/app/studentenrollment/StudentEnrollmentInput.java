package com.alvarengacarlos.unnamed.app.studentenrollment;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record StudentEnrollmentInput() {
    public static record Create(
        @NotNull UUID tuitionId,
        @NotNull UUID studentId,
        @NotNull UUID clazzId
    ) {
    }

    public static record Update(
        @NotNull Boolean isActive,
        @NotNull UUID tuitionId
    ) {
    }

    public static record GetStudentEnrollments(
        Optional<UUID> studentId,
        Optional<UUID> clazzId) {
        public GetStudentEnrollments {
            if (studentId == null) {
                studentId = Optional.empty();
            }

            if (clazzId == null) {
                clazzId = Optional.empty();
            }
        }
    }
}
