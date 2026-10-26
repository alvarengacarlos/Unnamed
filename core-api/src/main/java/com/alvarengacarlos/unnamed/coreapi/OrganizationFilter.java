package com.alvarengacarlos.unnamed.coreapi;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

@Component 
public class OrganizationFilter extends OncePerRequestFilter {
    public static final String ORGANIZATION_ID_MDC_KEY = "organizationId";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final HandlerExceptionResolver resolver;
    private final String organizationClaim;

    public OrganizationFilter(
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
        @Value("${unnamed.security.jwt.organization-claim}") String organizationClaim) {
        this.resolver = resolver;
        this.organizationClaim = organizationClaim;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest req, 
        HttpServletResponse res,
        FilterChain chain) throws ServletException, IOException {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            chain.doFilter(req, res);
            return;
        }

        final Object claim = jwtAuthentication.getToken().getClaims().get(organizationClaim);
        final Optional<UUID> organizationId = toOrganizationId(claim);
        if (organizationId.isEmpty()) {
            logger.warn(
                "Rejected {} {}: the {} claim of the token is {}, but it must be one organization UUID, "
                    + "as a string or as an array with one item",
                req.getMethod(),
                req.getRequestURI(),
                organizationClaim,
                claim == null ? "missing" : String.format("%s (%s)", claim, claim.getClass().getSimpleName()));
            resolver.resolveException(
                req,
                res, 
                null,
                new CommonException(
                    "invalid-organization-claim",
                    "Invalid organization claim",
                    String.format("Token has no valid %s claim", organizationClaim),
                    HttpStatus.FORBIDDEN));
            return;
        }

        // RequestLoggingFilter clears it after writing the access log.
        MDC.put(ORGANIZATION_ID_MDC_KEY, organizationId.get().toString());
        try {
            ScopedValue.where(OrganizationContext.ORGANIZATION, organizationId.get()).call(() -> {
                chain.doFilter(req, res);
                return null;
            });
        } catch (ServletException | IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * The organization of the claim of the token. The claim may be the UUID itself or an array with it, which is how
     * Keycloak sends its organizations. An array with more than one organization is refused: the API would not know
     * which to use.
     */
    private static Optional<UUID> toOrganizationId(Object claim) {
        final Object value = claim instanceof Collection<?> values && values.size() == 1
            ? values.iterator().next()
            : claim;
        if (!(value instanceof String uuid)) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(uuid));
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }
}
