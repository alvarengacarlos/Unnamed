package com.alvarengacarlos.unnamed.app.rank;

import java.time.Instant;
import java.util.UUID;

import com.alvarengacarlos.unnamed.app.rankcategory.RankCategoryOutput;

public record RankOutput(
    UUID id,
    String name,
    Byte order,
    RankCategoryOutput category,
    Instant createdAt,
    Instant updatedAt,
    UUID organizationId) {
    public static RankOutput from(Rank rank) {
        return new RankOutput(
            rank.getId(),
            rank.getName(),
            rank.getOrder(),
            RankCategoryOutput.from(rank.getCategory()),
            rank.getCreatedAt(),
            rank.getUpdatedAt(),
            rank.getOrganizationId());
    }
}
