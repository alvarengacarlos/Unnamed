package com.alvarengacarlos.unnamed.coreapi;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

/**
 * Answers and logs the requests whose bearer token is missing or cannot be accepted, so the log tells why: the reason
 * Spring Security gives, the claims of the token that the validation looks at and what the API expects.
 * The token itself is never logged, and neither are the claims about the user.
 */
@Component
public class AuthenticationErrors implements AuthenticationEntryPoint, AuthenticationFailureHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final BearerTokenResolver bearerTokenResolver = new DefaultBearerTokenResolver();
    private final HandlerExceptionResolver resolver;
    private final String jwkSetUri;
    private final String audience;
    private final String issuer;

    public AuthenticationErrors(
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
        @Value("${unnamed.security.jwt.jwk-set-uri}") String jwkSetUri,
        @Value("${unnamed.security.jwt.audience}") String audience,
        @Value("${unnamed.security.jwt.issuer}") String issuer) {
        this.resolver = resolver;
        this.jwkSetUri = jwkSetUri;
        this.audience = audience;
        this.issuer = issuer;
    }

    /**
     * Called when the request has a bearer token that could not be accepted.
     */
    @Override
    public void onAuthenticationFailure(
        HttpServletRequest req,
        HttpServletResponse res,
        AuthenticationException exception) {
        // The token was not judged at all, e.g. the keys of the issuer could not be fetched: a failure of the server.
        if (exception instanceof AuthenticationServiceException) {
            logger.error(
                "Could not validate the bearer token of {} {} with the keys of {}: {}",
                req.getMethod(),
                req.getRequestURI(),
                jwkSetUri,
                causes(exception),
                exception);
            resolver.resolveException(req, res, null, new CommonException(
                "token-validation-unavailable",
                "Could not validate the token",
                HttpStatus.INTERNAL_SERVER_ERROR));
            return;
        }

        commence(req, res, exception);
    }

    /**
     * Called when the request has no bearer token or has one that is not valid.
     */
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException exception) {
        resolveToken(req).ifPresentOrElse(
            token -> logger.warn(
                "Rejected the bearer token of {} {}: {}. Token: {}. Expected: {}",
                req.getMethod(),
                req.getRequestURI(),
                causes(exception),
                describe(token),
                expected()),
            () -> logger.info(
                "Rejected {} {}: {}",
                req.getMethod(),
                req.getRequestURI(),
                req.getHeader(HttpHeaders.AUTHORIZATION) == null
                    ? "no Authorization header"
                    : "the Authorization header is not a bearer token"));

        res.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        resolver.resolveException(req, res, null, new CommonException(
            "invalid-token",
            "Missing or invalid bearer token",
            HttpStatus.UNAUTHORIZED));
    }

    private Optional<String> resolveToken(HttpServletRequest req) {
        try {
            return Optional.ofNullable(bearerTokenResolver.resolve(req));
        } catch (AuthenticationException _) {
            return Optional.empty();
        }
    }

    // The messages of the exception and of its causes, which hold the reason (e.g. "The aud claim is not valid").
    private static String causes(Throwable exception) {
        final Set<String> messages = new LinkedHashSet<>();
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null) {
                messages.add(cause.getMessage());
            }
        }
        return String.join(" <- ", messages);
    }

    private static String describe(String token) {
        try {
            final JWT jwt = JWTParser.parse(token);
            final JWTClaimsSet claims = jwt.getJWTClaimsSet();
            final String keyId = jwt.getHeader() instanceof JWSHeader jwsHeader ? jwsHeader.getKeyID() : null;
            final Date expiresAt = claims.getExpirationTime();
            return String.format(
                "kid=%s, iss=%s, aud=%s, azp=%s, exp=%s",
                keyId,
                claims.getIssuer(),
                claims.getAudience(),
                claims.getClaim("azp"),
                expiresAt == null ? null : expiresAt.toInstant());
        } catch (ParseException _) {
            return "not a JWT";
        }
    }

    private String expected() {
        final String expectedAudience = String.format("aud containing %s", audience);
        return issuer.isBlank()
            ? expectedAudience
            : String.format("%s, iss=%s", expectedAudience, issuer);
    }
}
