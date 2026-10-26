package com.alvarengacarlos.unnamed.coreapi.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@TestComponent
public class ApiClient {
    public static final String AUDIENCE_CLAIM = "aud";
    private static final long TOKEN_LIFETIME_SECONDS = 300;
    private static final String PAGINATION_QUERY = "?page={page}&pageSize={pageSize}";
    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Value("${unnamed.security.jwt.audience}")
    private String audience;

    @Value("${unnamed.security.jwt.organization-claim}")
    private String organizationClaim;

    @Autowired
    private Environment environment;

    /**
     * The path with the page and pageSize query parameters as URI variables, in this order.
     */
    public static String paged(String path) {
        return path + PAGINATION_QUERY;
    }

    /**
     * Checks the Location of a resource created under the collection at the path.
     */
    public static void assertLocation(String location, String collectionPath) {
        assertThat(location).matches(Pattern.quote(collectionPath) + "/" + UUID_PATTERN);
    }

    public RestTestClient client() {
        return builder().build();
    }

    public RestTestClient client(UUID organizationId) {
        return client(createToken(
            Map.of(organizationClaim, organizationId.toString(), AUDIENCE_CLAIM, audience),
            Instant.now().plusSeconds(TOKEN_LIFETIME_SECONDS),
            TestJwks.key()));
    }

    public RestTestClient client(String token) {
        return builder()
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();
    }

    // The port is only known after the server starts, so it is read on each call.
    private RestTestClient.Builder<?> builder() {
        return RestTestClient.bindToServer()
            .baseUrl("http://localhost:" + environment.getRequiredProperty("local.server.port"))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
    }

    public String createToken(Map<String, Object> claims, Instant expiresAt, RSAKey key) {
        final JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder()
            .expirationTime(Date.from(expiresAt));
        claims.forEach(claimsSet::claim);
        final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(key.getKeyID())
            .build();
        final SignedJWT jwt = new SignedJWT(header, claimsSet.build());
        try {
            jwt.sign(new RSASSASigner(key));
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }

        return jwt.serialize();
    }

    public String getAudience() {
        return audience;
    }

    public String getOrganizationClaim() {
        return organizationClaim;
    }
}
