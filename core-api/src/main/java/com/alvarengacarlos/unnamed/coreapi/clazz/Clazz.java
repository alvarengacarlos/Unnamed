package com.alvarengacarlos.unnamed.coreapi.clazz;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import com.alvarengacarlos.unnamed.coreapi.teacher.Teacher;

public class Clazz {
    private final UUID id;
    private final String name;
    private final MartialArt martialArt;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Set<DayOfWeek> weekdays;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Teacher teacher;
    private final UUID organizationId;

    public static enum MartialArt {
        JIU_JITSU,
        JUDO,
        KARATE,
        MUAY_THAI,
        BOXING,
        TAEKWONDO,
        MMA,
        KICKBOXING,
        KUNG_FU
    }

    private Clazz(Builder builder) {
        id = builder.id;
        name = builder.name;
        martialArt = builder.martialArt;
        startTime = builder.startTime;
        endTime = builder.endTime;
        weekdays = builder.weekdays;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
        teacher = builder.teacher;
        organizationId = builder.organizationId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MartialArt getMartialArt() {
        return martialArt;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Set<DayOfWeek> getWeekdays() {
        return weekdays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    // updatedAt is not copied: the Builder starts it at now.
    public Builder toBuilder() {
        return new Builder()
            .id(id)
            .name(name)
            .martialArt(martialArt)
            .startTime(startTime)
            .endTime(endTime)
            .weekdays(weekdays)
            .createdAt(createdAt)
            .teacher(teacher)
            .organizationId(organizationId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String name;
        private MartialArt martialArt;
        private LocalTime startTime;
        private LocalTime endTime;
        private Set<DayOfWeek> weekdays;
        private Instant createdAt;
        private Instant updatedAt;
        private Teacher teacher;
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

        public Builder martialArt(MartialArt martialArt) {
            this.martialArt = martialArt;
            return this;
        }

        public Builder startTime(LocalTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder weekdays(Set<DayOfWeek> weekdays) {
            this.weekdays = weekdays;
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

        public Builder teacher(Teacher teacher) {
            this.teacher = teacher;
            return this;
        }

        public Builder organizationId(UUID organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Clazz build() {
            return new Clazz(this);
        }
    }
}
