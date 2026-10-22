package com.alvarengacarlos.unnamed.app.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CommonInput() {
    // The texts typed by the users, such as a name or an address. The columns of the database have the same limit.
    public static final int TEXT_MAX_LENGTH = 200;
    // The items of a bulk creation: each one is checked in the same query.
    public static final int BULK_MAX_SIZE = 200;
    // page * pageSize is the offset of the query, an Integer: a larger page would overflow it.
    public static final int PAGE_MAX = 1_000_000;
    public static final int PAGE_SIZE_MAX = 50;

    // page and pageSize are optional: a missing one gets its default in the constructor.
    public static record PaginationInput(
        @Min(0) @Max(PAGE_MAX) Integer page,
        @Min(1) @Max(PAGE_SIZE_MAX) Integer pageSize) {

        public PaginationInput {
            if (page == null) {
                page = 0;
            }
            if (pageSize == null) {
                pageSize = PAGE_SIZE_MAX;
            }
        }
    }
}
