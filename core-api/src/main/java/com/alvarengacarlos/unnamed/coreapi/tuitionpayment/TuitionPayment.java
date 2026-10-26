package com.alvarengacarlos.unnamed.coreapi.tuitionpayment;

import java.time.Instant;
import java.time.Month;
import java.util.UUID;

import com.alvarengacarlos.unnamed.coreapi.studentenrollment.StudentEnrollment;

public class TuitionPayment {
    private final UUID id;
    private final Month month;
    private final Short year;
    private final Boolean isPaid;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final StudentEnrollment studentEnrollment;
    private final UUID organizationId;

    private TuitionPayment(Builder builder) {
        id = builder.id;
        month = builder.month;
        year = builder.year;
        isPaid = builder.isPaid;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
        studentEnrollment = builder.studentEnrollment;
        organizationId = builder.organizationId;
    }

    public UUID getId() {
        return id;
    }

    public Month getMonth() {
        return month;
    }

    public Short getYear() {
        return year;
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

    public StudentEnrollment getStudentEnrollment() {
        return studentEnrollment;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    // updatedAt is not copied: the Builder starts it at now.
    public Builder toBuilder() {
        return new Builder()
            .id(id)
            .month(month)
            .year(year)
            .isPaid(isPaid)
            .createdAt(createdAt)
            .studentEnrollment(studentEnrollment)
            .organizationId(organizationId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Month month;
        private Short year;
        private Boolean isPaid;
        private Instant createdAt;
        private Instant updatedAt;
        private StudentEnrollment studentEnrollment;
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

        public Builder month(Month month) {
            this.month = month;
            return this;
        }

        public Builder year(Short year) {
            this.year = year;
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

        public Builder studentEnrollment(StudentEnrollment studentEnrollment) {
            this.studentEnrollment = studentEnrollment;
            return this;
        }

        public Builder organizationId(UUID organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public TuitionPayment build() {
            return new TuitionPayment(this);
        }
    }
}
