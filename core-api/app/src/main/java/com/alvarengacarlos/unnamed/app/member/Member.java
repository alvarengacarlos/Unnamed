package com.alvarengacarlos.unnamed.app.member;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.alvarengacarlos.unnamed.app.rank.Rank;

public class Member {
    private final UUID id;
    private final String name;
    private final String phone;
    private final String cpf;
    private final String address;
    private final Gender gender;
    private final BigDecimal weight;
    private final BigDecimal height;
    private final LocalDate birthDate;
    private final Boolean isPwd;
    private final Boolean isCompetitor;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Rank rank;
    private final UUID organizationId;

    private Member(Builder builder) {
        id = builder.id;
        name = builder.name;
        phone = builder.phone;
        cpf = builder.cpf;
        address = builder.address;
        gender = builder.gender;
        weight = builder.weight;
        height = builder.height;
        birthDate = builder.birthDate;
        isPwd = builder.isPwd;
        isCompetitor = builder.isCompetitor;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
        rank = builder.rank;
        organizationId = builder.organizationId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getCpf() {
        return cpf;
    }

    public String getAddress() {
        return address;
    }

    public Gender getGender() {
        return gender;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Boolean getIsPwd() {
        return isPwd;
    }

    public Boolean getIsCompetitor() {
        return isCompetitor;
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

    public UUID getOrganizationId() {
        return organizationId;
    }

    public static enum Gender {
        MALE,
        FEMALE
    }

    // updatedAt is not copied: the Builder starts it at now.
    public Builder toBuilder() {
        return new Builder()
            .id(id)
            .name(name)
            .phone(phone)
            .cpf(cpf)
            .address(address)
            .gender(gender)
            .weight(weight)
            .height(height)
            .birthDate(birthDate)
            .isPwd(isPwd)
            .isCompetitor(isCompetitor)
            .createdAt(createdAt)
            .rank(rank)
            .organizationId(organizationId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String name;
        private String phone;
        private String cpf;
        private String address;
        private Gender gender;
        private BigDecimal weight;
        private BigDecimal height;
        private LocalDate birthDate;
        private Boolean isPwd;
        private Boolean isCompetitor;
        private Instant createdAt;
        private Instant updatedAt;
        private Rank rank;
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

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder cpf(String cpf) {
            this.cpf = cpf;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder gender(Gender gender) {
            this.gender = gender;
            return this;
        }

        public Builder weight(BigDecimal weight) {
            this.weight = weight;
            return this;
        }

        public Builder height(BigDecimal height) {
            this.height = height;
            return this;
        }

        public Builder birthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Builder isPwd(Boolean isPwd) {
            this.isPwd = isPwd;
            return this;
        }

        public Builder isCompetitor(Boolean isCompetitor) {
            this.isCompetitor = isCompetitor;
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

        public Builder organizationId(UUID organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Member build() {
            return new Member(this);
        }
    }
}
