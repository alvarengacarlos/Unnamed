package com.alvarengacarlos.unnamed.coreapi.common;

import java.util.List;

/**
 * A page of a listing: its records, its number (from 0) and size, and the number of records of all the pages.
 */
public record Page<T>(
    List<T> content,
    Integer number,
    Integer size,
    Integer totalElements) {

    public Integer totalPages() {
        return Math.ceilDiv(totalElements, size);
    }

    // The same page with other records, such as its records converted after the read
    public <U> Page<U> withContent(List<U> content) {
        return new Page<>(content, number, size, totalElements);
    }
}
