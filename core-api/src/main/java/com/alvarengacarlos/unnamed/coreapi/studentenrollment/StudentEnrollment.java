package com.alvarengacarlos.unnamed.coreapi.studentenrollment;

import java.time.Instant;
import java.util.UUID;

import com.alvarengacarlos.unnamed.coreapi.clazz.Clazz;
import com.alvarengacarlos.unnamed.coreapi.student.Student;
import com.alvarengacarlos.unnamed.coreapi.tuition.Tuition;

public class StudentEnrollment {
    private final UUID id;
    private final Boolean isActive;
    private final Tuition tuition;
    private final Student student;
    private final Clazz clazz;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID organizationId;

    private StudentEnrollment(Builder builder) {
        id = builder.id;
        isActive = builder.isActive;
        tuition = builder.tuition;
        student = builder.student;
        clazz = builder.clazz;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
        organizationId = builder.organizationId;
    }

    public UUID getId() {
        return id;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public Tuition getTuition() {
        return tuition;
    }

    public Student getStudent() {
        return student;
    }

    public Clazz getClazz() {
        return clazz;
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
            .isActive(isActive)
            .tuition(tuition)
            .student(student)
            .clazz(clazz)
            .createdAt(createdAt)
            .organizationId(organizationId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Boolean isActive;
        private Tuition tuition;
        private Student student;
        private Clazz clazz;
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

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder tuition(Tuition tuition) {
            this.tuition = tuition;
            return this;
        }
        
        public Builder student(Student student) {
            this.student = student;
            return this;
        }

        public Builder clazz(Clazz clazz) {
            this.clazz = clazz;
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

        public StudentEnrollment build() {
            return new StudentEnrollment(this);
        }
    }
}
