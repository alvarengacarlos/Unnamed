package com.alvarengacarlos.unnamed.app.teacher;

import java.time.Instant;
import java.util.UUID;

import com.alvarengacarlos.unnamed.app.member.MemberOutput;

public record TeacherOutput(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    MemberOutput member,
    UUID organizationId) {
    public static TeacherOutput from(Teacher teacher) {
        return new TeacherOutput(
            teacher.getId(),
            teacher.getCreatedAt(),
            teacher.getUpdatedAt(),
            MemberOutput.from(teacher.getMember()),
            teacher.getOrganizationId());
    }
}
