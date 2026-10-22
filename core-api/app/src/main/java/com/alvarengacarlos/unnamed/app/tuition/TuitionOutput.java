package com.alvarengacarlos.unnamed.app.tuition;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TuitionOutput(
    UUID id,
    String name,
    BigDecimal price,
    Instant createdAt,
    Instant updatedAt,
    UUID organizationId) {
    public static TuitionOutput from(Tuition tuition) {
        return new TuitionOutput(
            tuition.getId(),
            tuition.getName(),
            tuition.getPrice(),
            tuition.getCreatedAt(),
            tuition.getUpdatedAt(),
            tuition.getOrganizationId());
    }
}
