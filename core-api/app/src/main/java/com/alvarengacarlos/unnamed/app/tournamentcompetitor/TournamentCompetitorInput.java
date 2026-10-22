package com.alvarengacarlos.unnamed.app.tournamentcompetitor;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import com.alvarengacarlos.unnamed.app.common.CommonInput;
import com.alvarengacarlos.unnamed.app.common.PaymentMethod;

public record TournamentCompetitorInput() {
    public static record Create(
        @NotNull PaymentMethod paymentMethod,
        @NotNull Boolean isPaid,
        @NotNull UUID tournamentId,
        @NotNull UUID memberId
    ) {
    }

    public static record Update(
        @NotNull PaymentMethod paymentMethod,
        @NotNull Boolean isPaid
    ) {
    }

    public static record CreateInBulk(
        @UniqueElements @NotEmpty @Size(max = CommonInput.BULK_MAX_SIZE) Set<@NotNull Create> bulk
    ) {}

    public static record GetTournamentCompetitors(
        Optional<UUID> tournamentId) {
        public GetTournamentCompetitors {
            if (tournamentId == null) {
                tournamentId = Optional.empty();
            }
        }
    }
}
