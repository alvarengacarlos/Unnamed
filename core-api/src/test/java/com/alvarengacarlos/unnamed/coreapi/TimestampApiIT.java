package com.alvarengacarlos.unnamed.coreapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alvarengacarlos.unnamed.coreapi.rankcategory.RankCategoryInput;
import com.alvarengacarlos.unnamed.coreapi.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.coreapi.testsupport.E2ETest;

/**
 * createdAt and updatedAt are instants in UTC, so a client converts them to its own time zone without guessing the
 * time zone of the server.
 */
@E2ETest
public class TimestampApiIT {
    private static final String UTC_INSTANT_PATTERN = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z";
    private final UUID organizationId = UUID.randomUUID();

    @Autowired
    private ApiClient api;

    @Test
    void shouldReturnTheAuditTimestampsInUtc() {
        api.client(organizationId).post()
            .uri("/rank-categories")
            .body(new RankCategoryInput.CreateUpdate("Kids"))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody()
            .jsonPath("$.createdAt").value(String.class, TimestampApiIT::assertUtcInstant)
            .jsonPath("$.updatedAt").value(String.class, TimestampApiIT::assertUtcInstant);
    }

    private static void assertUtcInstant(String value) {
        assertThat(value).matches(UTC_INSTANT_PATTERN);
    }
}
