package com.alvarengacarlos.unnamed.coreapi.tournamentmatch;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.alvarengacarlos.unnamed.coreapi.tournament.Tournament;
import com.alvarengacarlos.unnamed.coreapi.tournamentcompetitor.TournamentCompetitor;

public class TournamentMatch {
    private final UUID id;
    private final LocalDateTime when;
    private final String where;
    private final TournamentMatchResult result;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final TournamentCompetitor tournamentCompetitorA;
    private final TournamentCompetitor tournamentCompetitorB;
    private final Tournament tournament;
    private final UUID organizationId;

    public static enum TournamentMatchResult {
        PENDING,
        CANCELLED,
        NO_CONTEST,
        A,
        B,
        DRAW
    }

    private TournamentMatch(Builder builder) {
        id = builder.id;
        when = builder.when;
        where = builder.where;
        result = builder.result;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
        tournamentCompetitorA = builder.tournamentCompetitorA;
        tournamentCompetitorB = builder.tournamentCompetitorB;
        tournament = builder.tournament;
        organizationId = builder.organizationId;
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getWhen() {
        return when;
    }

    public String getWhere() {
        return where;
    }

    public TournamentMatchResult getResult() {
        return result;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public TournamentCompetitor getCompetitorA() {
        return tournamentCompetitorA;
    }

    public TournamentCompetitor getCompetitorB() {
        return tournamentCompetitorB;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    // updatedAt is not copied: the Builder starts it at now.
    public Builder toBuilder() {
        return new Builder()
            .id(id)
            .when(when)
            .where(where)
            .result(result)
            .createdAt(createdAt)
            .tournamentCompetitorA(tournamentCompetitorA)
            .tournamentCompetitorB(tournamentCompetitorB)
            .tournament(tournament)
            .organizationId(organizationId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private LocalDateTime when;
        private String where;
        private TournamentMatchResult result;
        private Instant createdAt;
        private Instant updatedAt;
        private TournamentCompetitor tournamentCompetitorA;
        private TournamentCompetitor tournamentCompetitorB;
        private Tournament tournament;
        private UUID organizationId;

        private Builder() {
            id = UUID.randomUUID();
            final Instant now = Instant.now();
            createdAt = now;
            updatedAt = now;
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder when(LocalDateTime when) {
            this.when = when;
            return this;
        }

        public Builder where(String where) {
            this.where = where;
            return this;
        }

        public Builder result(TournamentMatch.TournamentMatchResult result) {
            this.result = result;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder tournamentCompetitorA(TournamentCompetitor tournamentCompetitorA) {
            this.tournamentCompetitorA = tournamentCompetitorA;
            return this;
        }

        public Builder tournamentCompetitorB(TournamentCompetitor tournamentCompetitorB) {
            this.tournamentCompetitorB = tournamentCompetitorB;
            return this;
        }

        public Builder tournament(Tournament tournament) {
            this.tournament = tournament;
            return this;
        }

        public Builder organizationId(UUID organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public TournamentMatch build() {
            return new TournamentMatch(this);
        }
    }
}
