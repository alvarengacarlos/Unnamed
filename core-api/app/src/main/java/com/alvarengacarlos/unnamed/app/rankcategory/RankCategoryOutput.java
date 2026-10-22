package com.alvarengacarlos.unnamed.app.rankcategory;

import java.time.Instant;
import java.util.UUID;

public record RankCategoryOutput(
    UUID id,
    String name,
    Instant createdAt,
    Instant updatedAt,
    UUID organizationId) {
    public static RankCategoryOutput from(RankCategory rankCategory) {
        return new RankCategoryOutput(
            rankCategory.getId(), 
            rankCategory.getName(), 
            rankCategory.getCreatedAt(), 
            rankCategory.getUpdatedAt(),
            rankCategory.getOrganizationId());
    }
}
