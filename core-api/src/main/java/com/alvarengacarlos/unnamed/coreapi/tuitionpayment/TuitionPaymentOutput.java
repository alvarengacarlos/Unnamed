package com.alvarengacarlos.unnamed.coreapi.tuitionpayment;

import java.time.Instant;
import java.time.Month;
import java.util.UUID;

import com.alvarengacarlos.unnamed.coreapi.studentenrollment.StudentEnrollmentOutput;

public record TuitionPaymentOutput(
    UUID id,
    Month month,
    Short year,
    Boolean isPaid,
    Instant createdAt,
    Instant updatedAt,
    StudentEnrollmentOutput studentEnrollment,
    UUID organizationId
) {
    public static TuitionPaymentOutput from(TuitionPayment tuitionPayment) {
        return new TuitionPaymentOutput(
            tuitionPayment.getId(),
            tuitionPayment.getMonth(),
            tuitionPayment.getYear(),
            tuitionPayment.getIsPaid(),
            tuitionPayment.getCreatedAt(),
            tuitionPayment.getUpdatedAt(),
            StudentEnrollmentOutput.from(tuitionPayment.getStudentEnrollment()),
            tuitionPayment.getOrganizationId());
    }
}
