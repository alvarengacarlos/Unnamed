package com.alvarengacarlos.unnamed.coreapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;

import com.alvarengacarlos.unnamed.coreapi.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.coreapi.testsupport.E2ETest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Problem;

/**
 * When the keys of the issuer (JWKS) cannot be fetched, no token can be checked. That is a failure of the server, not
 * of the token, so the API must not answer it as an invalid token.
 */
@E2ETest
@TestPropertySource(properties = "unnamed.security.jwt.jwk-set-uri=http://localhost:1/unreachable-jwks.json")
@ExtendWith(OutputCaptureExtension.class)
public class TokenValidationUnavailableApiIT {
    private static final String PATH = "/members";

    @Autowired
    private ApiClient api;

    @Test
    void shouldReturnInternalServerErrorAndLogTheCauseWhenTheKeysCannotBeFetched(CapturedOutput output) {
        final Problem problem = api.client(UUID.randomUUID()).get()
            .uri(PATH)
            .exchange()
            .expectStatus()
            .isEqualTo(500)
            .expectBody(Problem.class)
            .returnResult()
            .getResponseBody();

        assertThat(problem.type()).isEqualTo(Problem.TYPE_PREFIX + "token-validation-unavailable");
        assertThat(output.getOut())
            .contains("Could not validate the bearer token of GET /members")
            .contains("http://localhost:1/unreachable-jwks.json");
    }
}
