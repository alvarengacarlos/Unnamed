package com.alvarengacarlos.unnamed.coreapi;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class OpenTelemetryLoggingConfig implements InitializingBean {
    private static final String APPENDER_NAME = "OPEN_TELEMETRY";
    private final ObjectProvider<OpenTelemetry> openTelemetryProvider;

    public OpenTelemetryLoggingConfig(ObjectProvider<OpenTelemetry> openTelemetryProvider) {
        this.openTelemetryProvider = openTelemetryProvider;
    }

    @Override
    public void afterPropertiesSet() {
        final OpenTelemetry openTelemetry = openTelemetryProvider.getIfAvailable();
        if (openTelemetry == null || !(LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext)) {
            return;
        }

        final Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        if (rootLogger.getAppender(APPENDER_NAME) != null) {
            rootLogger.detachAppender(APPENDER_NAME);
        }

        final OpenTelemetryAppender appender = new OpenTelemetryAppender();
        appender.setName(APPENDER_NAME);
        appender.setContext(loggerContext);
        appender.setOpenTelemetry(openTelemetry);
        appender.setCaptureKeyValuePairAttributes(true);
        appender.setCaptureCodeAttributes(true);
        appender.setCaptureMdcAttributes(String.join(
            ",",
            RequestLoggingFilter.REQUEST_ID_MDC_KEY,
            OrganizationFilter.ORGANIZATION_ID_MDC_KEY));
        appender.start();
        rootLogger.addAppender(appender);
    }
}
