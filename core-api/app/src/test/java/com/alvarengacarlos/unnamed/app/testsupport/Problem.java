package com.alvarengacarlos.unnamed.app.testsupport;

/**
 * The problem detail (RFC 9457) the API answers an error with. The instance is left out: it is the path of the
 * request.
 */
public record Problem(String type, String title, Integer status, String detail) {
    public static final String TYPE_PREFIX = "urn:unnamed:problem:";

    /**
     * A problem of the application whose detail is its title, as most of them are.
     */
    public static Problem of(String name, String title, int status) {
        return of(name, title, status, title);
    }

    public static Problem of(String name, String title, int status, String detail) {
        return new Problem(TYPE_PREFIX + name, title, status, detail);
    }
}
