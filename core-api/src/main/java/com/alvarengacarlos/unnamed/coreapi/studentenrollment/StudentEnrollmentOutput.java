package com.alvarengacarlos.unnamed.coreapi.studentenrollment;

import java.time.Instant;
import java.util.UUID;

import com.alvarengacarlos.unnamed.coreapi.clazz.ClazzOutput;
import com.alvarengacarlos.unnamed.coreapi.student.StudentOutput;
import com.alvarengacarlos.unnamed.coreapi.tuition.TuitionOutput;

public record StudentEnrollmentOutput(
    UUID id,
    Boolean isActive,
    TuitionOutput tuition,
    StudentOutput student,
    ClazzOutput clazz,
    Instant createdAt,
    Instant updatedAt,
    UUID organizationId) {
    public static StudentEnrollmentOutput from(StudentEnrollment studentEnrollment) {
        return new StudentEnrollmentOutput(
            studentEnrollment.getId(),
            studentEnrollment.getIsActive(),
            TuitionOutput.from(studentEnrollment.getTuition()),
            StudentOutput.from(studentEnrollment.getStudent()),
            ClazzOutput.from(studentEnrollment.getClazz()),
            studentEnrollment.getCreatedAt(),
            studentEnrollment.getUpdatedAt(),
            studentEnrollment.getOrganizationId());
    }
}
