package com.alvarengacarlos.unnamed.app.testsupport;

import java.util.List;

/**
 * The invalid-input problem, which lists the invalid fields of the request.
 */
public record ValidationProblem(String type, String title, Integer status, String detail, List<Field> errors) {
    public static record Field(String field, String message) {
    }
}
