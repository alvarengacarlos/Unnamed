package com.alvarengacarlos.unnamed.coreapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;

import com.alvarengacarlos.unnamed.coreapi.rankcategory.RankCategoryInput;
import com.alvarengacarlos.unnamed.coreapi.rankcategory.RankCategoryOutput;
import com.alvarengacarlos.unnamed.coreapi.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.coreapi.testsupport.E2ETest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Problem;
import com.alvarengacarlos.unnamed.coreapi.testsupport.TestJwks;

/**
 * Besides the answer, each rejection is logged with its reason, so a wrong configuration can be found from the log.
 */
@E2ETest
@ExtendWith(OutputCaptureExtension.class)
public class SecurityApiIT {
    private static final long TOKEN_LIFETIME_SECONDS = 300;
    private static final String PATH = "/rank-categories";
    private static final String NOT_AN_UUID = "organization";
    private static final String REJECTED_TOKEN_LOG = "Rejected the bearer token of GET " + PATH;
    private final UUID organizationId = UUID.randomUUID();

    @Autowired
    private ApiClient api;

    @Nested
    class ShouldReturnUnauthorized {
        @Test
        void whenThereIsNoToken(CapturedOutput output) {
            api.client().get()
                .uri(PATH)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectHeader()
                .valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("invalid-token", "Missing or invalid bearer token", 401));

            assertThat(output.getOut()).contains("Rejected GET " + PATH + ": no Authorization header");
        }

        @Test
        void whenTheTokenIsSignedWithAnotherKey(CapturedOutput output) {
            final String token = api.createToken(
                validClaims(),
                Instant.now().plusSeconds(TOKEN_LIFETIME_SECONDS),
                TestJwks.generateKey());

            expectUnauthorized(token);

            assertThat(output.getOut()).contains(REJECTED_TOKEN_LOG).contains("kid=");
        }

        @Test
        void whenTheTokenIsExpired(CapturedOutput output) {
            final String token = api.createToken(
                validClaims(),
                Instant.now().minusSeconds(TOKEN_LIFETIME_SECONDS),
                TestJwks.key());

            expectUnauthorized(token);

            assertThat(output.getOut()).contains(REJECTED_TOKEN_LOG).contains("expired");
        }

        @Test
        void whenTheTokenHasAnotherAudience(CapturedOutput output) {
            final Map<String, Object> claims = validClaims();
            claims.put(ApiClient.AUDIENCE_CLAIM, "another-api");
            final String token = token(claims);

            expectUnauthorized(token);

            assertThat(output.getOut())
                .contains(REJECTED_TOKEN_LOG)
                .contains("The aud claim is not valid")
                .contains("aud=[another-api]")
                .contains("Expected: aud containing " + api.getAudience())
                .doesNotContain(token);
        }

        @Test
        void whenTheTokenHasNoAudience() {
            final Map<String, Object> claims = validClaims();
            claims.remove(ApiClient.AUDIENCE_CLAIM);

            expectUnauthorized(token(claims));
        }

        @Test
        void whenTheTokenIsMalformed(CapturedOutput output) {
            expectUnauthorized("not-a-jwt");

            assertThat(output.getOut()).contains(REJECTED_TOKEN_LOG).contains("Token: not a JWT");
        }

        private void expectUnauthorized(String token) {
            api.client(token).get()
                .uri(PATH)
                .exchange()
                .expectStatus()
                .isUnauthorized();
        }
    }

    @Nested
    class ShouldReturnForbidden {
        @Test
        void whenTheTokenHasNoOrganizationClaim() {
            final Map<String, Object> claims = validClaims();
            claims.remove(api.getOrganizationClaim());

            api.client(token(claims)).get()
                .uri(PATH)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(Problem.class)
                .isEqualTo(Problem.of(
                    "invalid-organization-claim",
                    "Invalid organization claim",
                    403,
                    String.format("Token has no valid %s claim", api.getOrganizationClaim())));
        }

        @Test
        void whenTheOrganizationClaimIsNotAnUuid(CapturedOutput output) {
            final Map<String, Object> claims = validClaims();
            claims.put(api.getOrganizationClaim(), NOT_AN_UUID);

            api.client(token(claims)).get()
                .uri(PATH)
                .exchange()
                .expectStatus()
                .isForbidden();

            assertThat(output.getOut()).contains(String.format(
                "Rejected GET %s: the %s claim of the token is " + NOT_AN_UUID + " (String)",
                PATH,
                api.getOrganizationClaim()));
        }
    }

    @Nested
    class ShouldReturnForbiddenForAnArrayOfOrganizations {
        @Test
        void whenItHasMoreThanOne(CapturedOutput output) {
            final Map<String, Object> claims = validClaims();
            claims.put(
                api.getOrganizationClaim(),
                List.of(organizationId.toString(), UUID.randomUUID().toString()));

            expectForbidden(claims);

            assertThat(output.getOut()).contains(String.format(
                "Rejected GET %s: the %s claim of the token is [%s, ",
                PATH,
                api.getOrganizationClaim(),
                organizationId));
        }

        @Test
        void whenItIsEmpty() {
            final Map<String, Object> claims = validClaims();
            claims.put(api.getOrganizationClaim(), List.of());

            expectForbidden(claims);
        }

        @Test
        void whenItsItemIsNotAnUuid() {
            final Map<String, Object> claims = validClaims();
            claims.put(api.getOrganizationClaim(), List.of(NOT_AN_UUID));

            expectForbidden(claims);
        }

        private void expectForbidden(Map<String, Object> claims) {
            api.client(token(claims)).get()
                .uri(PATH)
                .exchange()
                .expectStatus()
                .isForbidden();
        }
    }

    @Test
    void shouldUseTheOrganizationOfAnArrayWithOneUuid() {
        final Map<String, Object> claims = validClaims();
        claims.put(api.getOrganizationClaim(), List.of(organizationId.toString()));

        final RankCategoryOutput output = api.client(token(claims)).post()
            .uri(PATH)
            .body(new RankCategoryInput.CreateUpdate("Adults"))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(RankCategoryOutput.class)
            .returnResult()
            .getResponseBody();

        assertThat(output.organizationId()).isEqualTo(organizationId);
    }

    @Test
    void shouldAcceptAValidToken() {
        api.client(token(validClaims())).get()
            .uri(ApiClient.paged(PATH), 0, 1)
            .exchange()
            .expectStatus()
            .isOk();
    }

    private Map<String, Object> validClaims() {
        final Map<String, Object> claims = new HashMap<>();
        claims.put(api.getOrganizationClaim(), organizationId.toString());
        claims.put(ApiClient.AUDIENCE_CLAIM, api.getAudience());
        return claims;
    }

    private String token(Map<String, Object> claims) {
        return api.createToken(claims, Instant.now().plusSeconds(TOKEN_LIFETIME_SECONDS), TestJwks.key());
    }
}
