package com.alvarengacarlos.unnamed.app.rank;

import java.time.Instant;
import java.util.UUID;

import com.alvarengacarlos.unnamed.app.rankcategory.RankCategory;

public class Rank {
    private final UUID id;
    private final String name;
    private final Byte order;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final RankCategory category;
    private final UUID organizationId;

    private Rank(Builder builder) {
        id = builder.id;
        name = builder.name;
        order = builder.order;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
        category = builder.category;
        organizationId = builder.organizationId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Byte getOrder() {
        return order;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public RankCategory getCategory() {
        return category;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    // updatedAt is not copied: the Builder starts it at now.
    public Builder toBuilder() {
        return new Builder()
            .id(id)
            .name(name)
            .order(order)
            .createdAt(createdAt)
            .category(category)
            .organizationId(organizationId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String name;
        private Byte order;
        private Instant createdAt;
        private Instant updatedAt;
        private RankCategory category;
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

        public Builder order(Byte order) {
            this.order = order;
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

        public Builder category(RankCategory category) {
            this.category = category;
            return this;
        }

        public Builder organizationId(UUID organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Rank build() {
            return new Rank(this);
        }
    }
}
