package com.alvarengacarlos.unnamed.app.common;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;

public final class Joins {
    private static final String ALIAS_SEPARATOR = "_";

    private Joins() {
    }

    public static String nested(String alias, String relation) {
        return alias + ALIAS_SEPARATOR + relation;
    }

    @SafeVarargs
    public static List<Field<?>> concat(List<Field<?>>... fields) {
        return Arrays.stream(fields)
            .flatMap(List::stream)
            .toList();
    }
}
