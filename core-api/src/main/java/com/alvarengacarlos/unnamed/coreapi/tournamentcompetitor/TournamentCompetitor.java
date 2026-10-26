package com.alvarengacarlos.unnamed.coreapi.tournamentcompetitor;

import java.time.Instant;
import java.util.UUID;

import com.alvarengacarlos.unnamed.coreapi.common.PaymentMethod;
import com.alvarengacarlos.unnamed.coreapi.member.Member;
import com.alvarengacarlos.unnamed.coreapi.tournament.Tournament;

public class TournamentCompetitor {
    private final UUID id;
    private final PaymentMethod paymentMethod;
    private final Boolean isPaid;
    private final Short points;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Tournament tournament;
    private final Member member;
    private final UUID organizationId;

    private TournamentCompetitor(Builder builder) {
        id = builder.id;
        paymentMethod = builder.paymentMethod;
        isPaid = builder.isPaid;
        points = builder.points;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
        tournament = builder.tournament;
        member = builder.member;
        organizationId = builder.organizationId;
    }

    public UUID getId() {
        return id;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Boolean getIsPaid() {
        return isPaid;
    }

    public Short getPoints() {
        return points;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public Member getMember() {
        return member;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    // updatedAt is not copied: the Builder starts it at now.
    public Builder toBuilder() {
        return new Builder()
            .id(id)
            .paymentMethod(paymentMethod)
            .isPaid(isPaid)
            .points(points)
            .createdAt(createdAt)
            .tournament(tournament)
            .member(member)
            .organizationId(organizationId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private PaymentMethod paymentMethod;
        private Boolean isPaid;
        private Short points;
        private Instant createdAt;
        private Instant updatedAt;
        private Tournament tournament;
        private Member member;
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

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder isPaid(Boolean isPaid) {
            this.isPaid = isPaid;
            return this;
        }

        public Builder points(Short points) {
            this.points = points;
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

        public Builder tournament(Tournament tournament) {
            this.tournament = tournament;
            return this;
        }

        public Builder member(Member member) {
            this.member = member;
            return this;
        }

        public Builder organizationId(UUID organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public TournamentCompetitor build() {
            return new TournamentCompetitor(this);
        }
    }
}
