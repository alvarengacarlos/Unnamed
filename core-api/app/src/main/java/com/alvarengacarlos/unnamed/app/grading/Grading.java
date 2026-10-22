package com.alvarengacarlos.unnamed.app.grading;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class Grading {
    private final UUID id;
    private final String name;
    private final LocalDateTime when;
    private final String where;
    private final LocalDate registrationPeriodStart;
    private final LocalDate registrationPeriodEnd;
    private final Set<GradingPrice> gradingPrices;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID organizationId;

    private Grading(Builder builder) {
        id = builder.id;
        name = builder.name;
        when = builder.when;
        where = builder.where;
        registrationPeriodStart = builder.registrationPeriodStart;
        registrationPeriodEnd = builder.registrationPeriodEnd;
        gradingPrices = builder.gradingPrices;
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

    public LocalDateTime getWhen() {
        return when;
    }

    public String getWhere() {
        return where;
    }

    public LocalDate getRegistrationPeriodStart() {
        return registrationPeriodStart;
    }

    public LocalDate getRegistrationPeriodEnd() {
        return registrationPeriodEnd;
    }

    public Set<GradingPrice> getGradingPrices() {
        return gradingPrices;
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
            .when(when)
            .where(where)
            .registrationPeriodStart(registrationPeriodStart)
            .registrationPeriodEnd(registrationPeriodEnd)
            .gradingPrices(gradingPrices)
            .createdAt(createdAt)
            .organizationId(organizationId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String name;
        private LocalDateTime when;
        private String where;
        private LocalDate registrationPeriodStart;
        private LocalDate registrationPeriodEnd;
        private Set<GradingPrice> gradingPrices;
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

        public Builder when(LocalDateTime when) {
            this.when = when;
            return this;
        }

        public Builder where(String where) {
            this.where = where;
            return this;
        }

        public Builder registrationPeriodStart(LocalDate registrationPeriodStart) {
            this.registrationPeriodStart = registrationPeriodStart;
            return this;
        }

        public Builder registrationPeriodEnd(LocalDate registrationPeriodEnd) {
            this.registrationPeriodEnd = registrationPeriodEnd;
            return this;
        }

        public Builder gradingPrices(Set<GradingPrice> gradingPrices) {
            this.gradingPrices = gradingPrices;
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

        public Grading build() {
            return new Grading(this);
        }
    }
}
