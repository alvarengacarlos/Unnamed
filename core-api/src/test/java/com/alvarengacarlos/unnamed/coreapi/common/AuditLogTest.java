package com.alvarengacarlos.unnamed.coreapi.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class AuditLogTest {
    private static final String SUBJECT = "user-sub";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBeTheSubOfTheToken() {
        final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(SUBJECT)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThat(AuditLog.subject()).isEqualTo(SUBJECT);
    }

    @Test
    void shouldBeTheSystemWithoutARequest() {
        assertThat(AuditLog.subject()).isEqualTo(AuditLog.SYSTEM_SUBJECT);
    }
}
