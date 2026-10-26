package com.alvarengacarlos.unnamed.coreapi.grading;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record GradingOutput(
    UUID id,
    String name,
    LocalDateTime when,
    String where,
    LocalDate registrationPeriodStart,
    LocalDate registrationPeriodEnd,
    Set<GradingPriceOutput> gradingPrices,
    Instant createdAt,
    Instant updatedAt,
    UUID organizationId) {
    public static GradingOutput from(Grading grading) {
        return new GradingOutput(
            grading.getId(), 
            grading.getName(),
            grading.getWhen(), 
            grading.getWhere(), 
            grading.getRegistrationPeriodStart(),
            grading.getRegistrationPeriodEnd(),
            grading.getGradingPrices().stream().map(GradingPriceOutput::from).collect(Collectors.toSet()),
            grading.getCreatedAt(), 
            grading.getUpdatedAt(),
            grading.getOrganizationId());
    }
}
