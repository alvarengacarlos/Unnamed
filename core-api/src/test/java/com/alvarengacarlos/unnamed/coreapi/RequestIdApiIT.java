package com.alvarengacarlos.unnamed.coreapi;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alvarengacarlos.unnamed.coreapi.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.coreapi.testsupport.E2ETest;

@E2ETest
public class RequestIdApiIT {
    private static final String PATH = "/members";
    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private final UUID organizationId = UUID.randomUUID();

    @Autowired
    private ApiClient api;

    @Nested
    class ShouldGenerateARequestId {
        @Test
        void whenTheHeaderIsMissing() {
            api.client(organizationId).get()
                .uri(PATH)
                .exchange()
                .expectHeader()
                .valueMatches(RequestLoggingFilter.REQUEST_ID_HEADER, UUID_PATTERN);
        }

        @Test
        void whenTheHeaderIsInvalid() {
            api.client(organizationId).get()
                .uri(PATH)
                .header(RequestLoggingFilter.REQUEST_ID_HEADER, "invalid request id")
                .exchange()
                .expectHeader()
                .valueMatches(RequestLoggingFilter.REQUEST_ID_HEADER, UUID_PATTERN);
        }

        @Test
        void whenTheRequestIsUnauthenticated() {
            api.client().get()
                .uri(PATH)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectHeader()
                .valueMatches(RequestLoggingFilter.REQUEST_ID_HEADER, UUID_PATTERN);
        }
    }

    @Test
    void shouldReuseAValidRequestId() {
        final String requestId = "client-request-id-123";

        api.client(organizationId).get()
            .uri(PATH)
            .header(RequestLoggingFilter.REQUEST_ID_HEADER, requestId)
            .exchange()
            .expectHeader()
            .valueEquals(RequestLoggingFilter.REQUEST_ID_HEADER, requestId);
    }
}
