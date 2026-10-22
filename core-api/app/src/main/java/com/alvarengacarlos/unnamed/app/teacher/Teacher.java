package com.alvarengacarlos.unnamed.app.teacher;

import java.time.Instant;
import java.util.UUID;

import com.alvarengacarlos.unnamed.app.member.Member;

public class Teacher {
    private UUID id;
    private Member member;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID organizationId;

    private Teacher(Builder builder) {
        id = builder.id;
        member = builder.member;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
        organizationId = builder.organizationId;
    }

    public UUID getId() {
        return id;
    }

    public Member getMember() {
        return member;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Member member;
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

        public Builder member(Member member) {
            this.member = member;
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

        public Teacher build() {
            return new Teacher(this);
        }
    }
}
