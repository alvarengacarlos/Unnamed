package com.alvarengacarlos.unnamed.app.rankcategory;

import java.time.Instant;
import java.util.UUID;

public class RankCategory {
    private final UUID id;
    private final String name;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID organizationId;

    private RankCategory(Builder builder) {
        id = builder.id;
        name = builder.name;
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
            .createdAt(createdAt)
            .organizationId(organizationId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String name;
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

        public RankCategory build() {
            return new RankCategory(this);
        }
    }
}
