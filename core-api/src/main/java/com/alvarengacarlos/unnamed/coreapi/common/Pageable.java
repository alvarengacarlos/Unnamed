package com.alvarengacarlos.unnamed.coreapi.common;

/**
 * The page a listing reads: its number (from 0) and size.
 */
public record Pageable(Integer page, Integer pageSize) {
    public static Pageable from(CommonInput.PaginationInput input) {
        return new Pageable(input.page(), input.pageSize());
    }

    public Integer offset() {
        return page * pageSize;
    }

    public Integer limit() {
        return pageSize;
    }
}
