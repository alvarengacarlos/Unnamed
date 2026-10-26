package com.alvarengacarlos.unnamed.coreapi.gradingcandidate;

import java.time.Instant;
import java.util.UUID;

import com.alvarengacarlos.unnamed.coreapi.common.PaymentMethod;
import com.alvarengacarlos.unnamed.coreapi.grading.Grading;
import com.alvarengacarlos.unnamed.coreapi.member.Member;
import com.alvarengacarlos.unnamed.coreapi.rank.Rank;

public class GradingCandidate {
    private final UUID id;
    private final PaymentMethod paymentMethod;
    private final Boolean isPaid;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Rank rank;
    private final Grading grading;
    private final Member member;
    private final UUID organizationId;

    private GradingCandidate(Builder builder) {
        id = builder.id;
        paymentMethod = builder.paymentMethod;
        isPaid = builder.isPaid;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
        rank = builder.rank;
        grading = builder.grading;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Rank getRank() {
        return rank;
    }

    public Grading getGrading() {
        return grading;
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
            .createdAt(createdAt)
            .rank(rank)
            .grading(grading)
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
        private Instant createdAt;
        private Instant updatedAt;
        private Rank rank;
        private Grading grading;
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

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder rank(Rank rank) {
            this.rank = rank;
            return this;
        }

        public Builder grading(Grading grading) {
            this.grading = grading;
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

        public GradingCandidate build() {
            return new GradingCandidate(this);
        }
    }
}
