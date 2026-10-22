package com.alvarengacarlos.unnamed.app;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private final Logger accessLogger = LoggerFactory.getLogger(getClass());

    @Override
    protected void doFilterInternal(
        HttpServletRequest req,
        HttpServletResponse res,
        FilterChain chain) throws ServletException, IOException {
        final String requestId = resolveRequestId(req);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        res.setHeader(REQUEST_ID_HEADER, requestId);

        final long start = System.nanoTime();
        Integer status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        try {
            chain.doFilter(req, res);
            status = res.getStatus();
        } finally {
            final long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            accessLogger.atInfo()
                .addKeyValue("method", req.getMethod())
                .addKeyValue("path", req.getRequestURI())
                .addKeyValue("status", status)
                .addKeyValue("durationMs", durationMs)
                .log("{} {} {} {}ms", req.getMethod(), req.getRequestURI(), status, durationMs);
            // Also clears the keys added by inner filters, such as OrganizationFilter.
            MDC.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest req) {
        final String requestId = req.getHeader(REQUEST_ID_HEADER);
        if (Objects.nonNull(requestId) && VALID_REQUEST_ID.matcher(requestId).matches()) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }
}
