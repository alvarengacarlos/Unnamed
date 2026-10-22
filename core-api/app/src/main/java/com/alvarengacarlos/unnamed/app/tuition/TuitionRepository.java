package com.alvarengacarlos.unnamed.app.tuition;

import static com.alvarengacarlos.unnamed.app.jooq.Tables.TUITIONS;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Repository;

import com.alvarengacarlos.unnamed.app.OrganizationContext;
import com.alvarengacarlos.unnamed.app.common.Page;
import com.alvarengacarlos.unnamed.app.common.Pageable;
import com.alvarengacarlos.unnamed.app.common.Pagination;
import com.alvarengacarlos.unnamed.app.jooq.tables.Tuitions;
import com.alvarengacarlos.unnamed.app.jooq.tables.records.TuitionsRecord;

@Repository
public class TuitionRepository {
    private final DSLContext dsl;

    public TuitionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void save(Tuition tuition) {
        dsl.insertInto(TUITIONS)
            .set(TUITIONS.ID, tuition.getId())
            .set(TUITIONS.NAME, tuition.getName())
            .set(TUITIONS.PRICE, tuition.getPrice())
            .set(TUITIONS.CREATED_AT, tuition.getCreatedAt())
            .set(TUITIONS.UPDATED_AT, tuition.getUpdatedAt())
            .set(TUITIONS.ORGANIZATION_ID, OrganizationContext.require())
            .execute();
    }

    public void update(Tuition tuition) {
        dsl.update(TUITIONS)
            .set(TUITIONS.NAME, tuition.getName())
            .set(TUITIONS.PRICE, tuition.getPrice())
            .set(TUITIONS.UPDATED_AT, tuition.getUpdatedAt())
            .where(TUITIONS.ID.eq(tuition.getId()))
            .and(TUITIONS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    public Optional<Tuition> read(UUID id) {
        return dsl.selectFrom(TUITIONS)
            .where(TUITIONS.ID.eq(id))
            .and(TUITIONS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetchOptional(TuitionRepository::toTuition);
    }

    public Page<Tuition> read(Pageable pageable) {
        return Pagination.page(
            dsl.select(TUITIONS.fields())
                .from(TUITIONS)
                .where(TUITIONS.ORGANIZATION_ID.eq(OrganizationContext.require())),
            pageable,
            Pagination.newestFirst(TUITIONS.CREATED_AT, TUITIONS.ID),
            record -> toTuition(record.into(TUITIONS)));
    }

    public void delete(UUID id) {
        dsl.deleteFrom(TUITIONS)
            .where(TUITIONS.ID.eq(id))
            .and(TUITIONS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    public static List<Field<?>> fields(String alias) {
        return Arrays.asList(TUITIONS.as(alias).fields());
    }

    public static SelectJoinStep<Record> join(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final Tuitions tuition = TUITIONS.as(alias);
        return select.join(tuition).on(tuition.ID.eq(foreignKey));
    }

    public static Tuition toTuition(Record record, String alias) {
        return toTuition(record.into(TUITIONS.as(alias)));
    }

    private static Tuition toTuition(TuitionsRecord tuition) {
        return Tuition.builder()
            .id(tuition.getId())
            .name(tuition.getName())
            .price(tuition.getPrice())
            .createdAt(tuition.getCreatedAt())
            .updatedAt(tuition.getUpdatedAt())
            .organizationId(tuition.getOrganizationId())
            .build();
    }
}
