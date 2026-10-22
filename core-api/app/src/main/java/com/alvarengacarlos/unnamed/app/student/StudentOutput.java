package com.alvarengacarlos.unnamed.app.student;

import java.time.Instant;
import java.util.UUID;

import com.alvarengacarlos.unnamed.app.member.MemberOutput;

public record StudentOutput(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    MemberOutput member,
    UUID organizationId) {
    public static StudentOutput from(Student student) {
        return new StudentOutput(
            student.getId(),
            student.getCreatedAt(),
            student.getUpdatedAt(),
            MemberOutput.from(student.getMember()),
            student.getOrganizationId());
    }
}
