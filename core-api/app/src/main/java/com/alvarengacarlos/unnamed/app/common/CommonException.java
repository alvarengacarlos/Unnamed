package com.alvarengacarlos.unnamed.app.common;

import java.net.URI;

import org.springframework.http.HttpStatus;

/**
 * An error the API answers with a problem detail (RFC 9457). The type identifies the error for clients and never
 * changes; the title describes the type; the detail, which defaults to the title, describes this occurrence.
 */
public class CommonException extends RuntimeException {
    public static final String TYPE_PREFIX = "urn:unnamed:problem:";
    public final URI type;
    public final String title;
    public final HttpStatus httpStatus;

    public CommonException(String type, String title, HttpStatus httpStatus) {
        this(type, title, title, httpStatus);
    }

    public CommonException(String type, String title, String detail, HttpStatus httpStatus) {
        super(detail);
        this.type = type(type);
        this.title = title;
        this.httpStatus = httpStatus;
    }

    public static URI type(String name) {
        return URI.create(TYPE_PREFIX + name);
    }
}
