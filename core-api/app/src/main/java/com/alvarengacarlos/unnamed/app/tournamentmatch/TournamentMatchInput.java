package com.alvarengacarlos.unnamed.app.tournamentmatch;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import com.alvarengacarlos.unnamed.app.common.CommonInput;

public record TournamentMatchInput() {
    public static record Create(
        @NotNull LocalDateTime when,
        @NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String where,
        @NotNull UUID tournamentCompetitorAId,
        @NotNull UUID tournamentCompetitorBId,
        @NotNull UUID tournamentId
    ) {
    }

    public static record Update(
        @NotNull LocalDateTime when,
        @NotBlank @Size(max = CommonInput.TEXT_MAX_LENGTH) String where,
        @NotNull TournamentMatch.TournamentMatchResult result
    ) {
    }

    public static record CreateInBulk(
        @UniqueElements @NotEmpty @Size(max = CommonInput.BULK_MAX_SIZE) Set<@NotNull Create> bulk
    ) {}

    public static record GetTournamentMatches(
        Optional<UUID> tournamentId) {
        public GetTournamentMatches {
            if (tournamentId == null) {
                tournamentId = Optional.empty();
            }
        }
    }
}
