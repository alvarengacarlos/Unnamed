package com.alvarengacarlos.unnamed.coreapi.testsupport;

import java.util.List;

import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

public final class Pages {
    private Pages() {
    }

    /**
     * The only page of a list: page 0, of the size requested, holding the whole list.
     */
    public static <T> CommonOutput.PaginationOutput<T> onlyPage(List<T> content, int size) {
        return new CommonOutput.PaginationOutput<>(
            content,
            new CommonOutput.PageMetadata(0, size, content.size(), 1));
    }
}
