package com.alvarengacarlos.unnamed.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.alvarengacarlos.unnamed.app.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.app.testsupport.E2ETest;

@E2ETest
public class PublicPathApiIT {
    @Autowired
    private ApiClient api;

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
    void shouldReturnHealthWithoutAuthentication(String path) {
        api.client().get()
            .uri(path)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void shouldNotExposeOtherActuatorEndpoints() {
        api.client().get()
            .uri("/actuator/env")
            .exchange()
            .expectStatus()
            .isUnauthorized();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/swagger-ui/index.html"})
    void shouldExposeTheApiDocsWithoutAuthentication(String path) {
        api.client().get()
            .uri(path)
            .exchange()
            .expectStatus()
            .isOk();
    }
}
