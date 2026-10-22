package com.alvarengacarlos.unnamed.app.testsupport;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.sun.net.httpserver.HttpServer;

public final class TestJwks {
    private static final int KEY_SIZE = 2048;
    private static final String PATH = "/jwks.json";
    private static final RSAKey KEY = generateKey();
    private static String uri;

    private TestJwks() {
    }

    public static RSAKey key() {
        return KEY;
    }

    public static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(KEY_SIZE)
                .keyID(UUID.randomUUID().toString())
                .generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public static synchronized String start() {
        if (uri != null) {
            return uri;
        }

        final byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(PATH, exchange -> {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(body);
                }
            });
            server.start();
            uri = "http://localhost:" + server.getAddress().getPort() + PATH;
            return uri;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
