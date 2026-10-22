package com.alvarengacarlos.unnamed.app.common;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;

public final class Pagination {
    private static final Field<Integer> TOTAL_ELEMENTS = DSL.count().over().as("total_elements");

    private Pagination() {
    }

    /**
     * The order of the listings: the most recent records first. The id breaks the ties, so a record does not move
     * between pages when two of them were created at the same instant.
     */
    public static List<OrderField<?>> newestFirst(Field<Instant> createdAt, Field<UUID> id) {
        return List.of(createdAt.desc(), id.desc());
    }

    public static <R extends Record, T> Page<T> page(
        SelectConditionStep<R> select,
        Pageable pageable,
        List<OrderField<?>> orderBy,
        RecordMapper<? super R, T> mapper) {
        select.getQuery().addSelect(TOTAL_ELEMENTS);
        select.getQuery().addOrderBy(orderBy);
        final Result<R> result = select
            .limit(pageable.limit())
            .offset(pageable.offset())
            .fetch();

        final int totalElements;
        if (result.isNotEmpty()) {
            totalElements = result.get(0).get(TOTAL_ELEMENTS);
        } else if (pageable.offset() > 0) {
            // A page past the end has no rows to carry the window count, so read it from the first row
            select.getQuery().addLimit(0, 1);
            totalElements = select.fetchOptional()
                .map(record -> record.get(TOTAL_ELEMENTS))
                .orElse(0);
        } else {
            totalElements = 0;
        }

        final List<T> content = result.map(mapper);
        return new Page<>(content, pageable.page(), pageable.pageSize(), totalElements);
    }
}
