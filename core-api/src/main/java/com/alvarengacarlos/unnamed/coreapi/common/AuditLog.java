package com.alvarengacarlos.unnamed.coreapi.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Who changed what: each create, update and delete is logged with the sub of the token that asked for it, as
 * "<sub> updating tuition payment <id>". The lines carry ids only, never the values sent, which may be personal data.
 * They all come from this logger, so a log collector can pick them out by its name.
 */
public final class AuditLog {
    // What runs without a request, as MonthlyTuitionPaymentJob
    public static final String SYSTEM_SUBJECT = "system";
    private static final Logger logger = LoggerFactory.getLogger(AuditLog.class);

    private AuditLog() {
    }

    public static void log(String action, Object... args) {
        if (!logger.isInfoEnabled()) {
            return;
        }

        final Object[] subjectAndArgs = new Object[args.length + 1];
        subjectAndArgs[0] = subject();
        System.arraycopy(args, 0, subjectAndArgs, 1, args.length);
        logger.info("{} " + action, subjectAndArgs);
    }

    // The sub of the token, which JwtAuthenticationToken uses as its name
    public static String subject() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken jwtAuthentication
            ? jwtAuthentication.getName()
            : SYSTEM_SUBJECT;
    }
}
