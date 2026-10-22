package com.alvarengacarlos.unnamed.app.common;

import java.util.List;
import java.util.function.Function;

public record CommonOutput() {
    public static record PaginationOutput<T>(List<T> content, PageMetadata page) {
        public static <E, T> PaginationOutput<T> from(Page<E> page, Function<E, T> toOutput) {
            return new PaginationOutput<>(
                page.content().stream().map(toOutput).toList(),
                new PageMetadata(page.number(), page.size(), page.totalElements(), page.totalPages()));
        }
    }

    /**
     * Where a page is in the list: its number (from 0), its size and the totals of the list.
     */
    public static record PageMetadata(Integer number, Integer size, Integer totalElements, Integer totalPages) {
    }

    /**
     * An invalid field of a request, listed in the errors property of an invalid-input problem.
     */
    public static record InvalidField(String field, String message) {
    }
}
