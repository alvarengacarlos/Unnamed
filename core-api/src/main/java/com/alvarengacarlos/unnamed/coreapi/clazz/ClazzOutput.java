package com.alvarengacarlos.unnamed.coreapi.clazz;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import com.alvarengacarlos.unnamed.coreapi.teacher.TeacherOutput;

public record ClazzOutput(
    UUID id,
    String name,
    Clazz.MartialArt martialArt,
    LocalTime startTime,
    LocalTime endTime,
    Set<DayOfWeek> weekdays,
    Instant createdAt,
    Instant updatedAt,
    TeacherOutput teacher,
    UUID organizationId) {
    public static ClazzOutput from(Clazz clazz) {
        return new ClazzOutput(
            clazz.getId(),
            clazz.getName(),
            clazz.getMartialArt(),
            clazz.getStartTime(), 
            clazz.getEndTime(), 
            clazz.getWeekdays(), 
            clazz.getCreatedAt(),
            clazz.getUpdatedAt(),
            TeacherOutput.from(clazz.getTeacher()),
            clazz.getOrganizationId());
    }
}
