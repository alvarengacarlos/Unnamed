package com.alvarengacarlos.unnamed.coreapi.testsupport;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Starts the local JWKS server and points the application to it before the Spring context is created.
 */
public class JwksExtension implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        System.setProperty("unnamed.security.jwt.jwk-set-uri", TestJwks.start());
    }
}
