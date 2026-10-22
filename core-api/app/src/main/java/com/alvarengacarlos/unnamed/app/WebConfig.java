package com.alvarengacarlos.unnamed.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${unnamed.cors.allowed-origins}")
    private String[] corsAllowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        final String allowAll = "*";
        registry.addMapping("/**")
            .allowedOriginPatterns(corsAllowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders(allowAll)
            .exposedHeaders(RequestLoggingFilter.REQUEST_ID_HEADER, HttpHeaders.LOCATION)
            .maxAge(3600);
    }
}
