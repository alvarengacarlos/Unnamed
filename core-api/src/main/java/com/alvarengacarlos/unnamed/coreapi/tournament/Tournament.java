package com.alvarengacarlos.unnamed.coreapi.tournament;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class Tournament {
    private final UUID id;
    private final String name;
    private final TournamentFormat format;
    private final TournamentDivisionCriterion tournamentDivisionCriterion;
    private final BigDecimal participationPrice;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final String where;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID organizationId;

    public static enum TournamentFormat {
        TITLE_MATCH,
        SINGLE_ELIMINATION,
        ROUND_ROBIN
    }
    
    private Tournament(Builder builder) {
        id = builder.id;
        name = builder.name;
        format = builder.format;
        tournamentDivisionCriterion = builder.tournamentDivisionCriterion;
        participationPrice = builder.participationPrice;
        startDateTime = builder.startDateTime;
        endDateTime = builder.endDateTime;
        where = builder.where;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
        organizationId = builder.organizationId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TournamentFormat getFormat() {
        return format;
    }

    public TournamentDivisionCriterion getTournamentDivisionCriterion() {
        return tournamentDivisionCriterion;
    }

    public BigDecimal getParticipationPrice() {
        return participationPrice;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public String getWhere() {
        return where;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    // updatedAt is not copied: the Builder starts it at now.
    public Builder toBuilder() {
        return new Builder()
            .id(id)
            .name(name)
            .format(format)
            .tournamentDivisionCriterion(tournamentDivisionCriterion)
            .participationPrice(participationPrice)
            .startDateTime(startDateTime)
            .endDateTime(endDateTime)
            .where(where)
            .createdAt(createdAt)
            .organizationId(organizationId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String name;
        private TournamentFormat format;
        private TournamentDivisionCriterion tournamentDivisionCriterion;
        private BigDecimal participationPrice;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
        private String where;
        private Instant createdAt;
        private Instant updatedAt;
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

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder format(TournamentFormat format) {
            this.format = format;
            return this;
        }

        public Builder tournamentDivisionCriterion(TournamentDivisionCriterion tournamentDivisionCriterion) {
            this.tournamentDivisionCriterion = tournamentDivisionCriterion;
            return this;
        }

        public Builder participationPrice(BigDecimal participationPrice) {
            this.participationPrice = participationPrice;
            return this;
        }

        public Builder startDateTime(LocalDateTime startDateTime) {
            this.startDateTime = startDateTime;
            return this;
        }

        public Builder endDateTime(LocalDateTime endDateTime) {
            this.endDateTime = endDateTime;
            return this;
        }

        public Builder where(String where) {
            this.where = where;
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

        public Builder organizationId(UUID organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Tournament build() {
            return new Tournament(this);
        }
    }
}
