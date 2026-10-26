package com.alvarengacarlos.unnamed.coreapi.rankcategory;

import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.RANK_CATEGORIES;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Repository;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.common.Pagination;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.RankCategories;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.records.RankCategoriesRecord;

@Repository
public class RankCategoryRepository {
    private final DSLContext dsl;

    public RankCategoryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void save(RankCategory rankCategory) {
        dsl.insertInto(RANK_CATEGORIES)
            .set(RANK_CATEGORIES.ID, rankCategory.getId())
            .set(RANK_CATEGORIES.NAME, rankCategory.getName())
            .set(RANK_CATEGORIES.CREATED_AT, rankCategory.getCreatedAt())
            .set(RANK_CATEGORIES.UPDATED_AT, rankCategory.getUpdatedAt())
            .set(RANK_CATEGORIES.ORGANIZATION_ID, OrganizationContext.require())
            .execute();
    }

    public void update(RankCategory rankCategory) {
        dsl.update(RANK_CATEGORIES)
            .set(RANK_CATEGORIES.NAME, rankCategory.getName())
            .set(RANK_CATEGORIES.UPDATED_AT, rankCategory.getUpdatedAt())
            .where(RANK_CATEGORIES.ID.eq(rankCategory.getId()))
            .and(RANK_CATEGORIES.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    public Optional<RankCategory> read(UUID id) {
        return dsl.selectFrom(RANK_CATEGORIES)
            .where(RANK_CATEGORIES.ID.eq(id))
            .and(RANK_CATEGORIES.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetchOptional(RankCategoryRepository::toRankCategory);
    }

    public Page<RankCategory> read(Pageable pageable) {
        return Pagination.page(
            dsl.select(RANK_CATEGORIES.fields())
                .from(RANK_CATEGORIES)
                .where(RANK_CATEGORIES.ORGANIZATION_ID.eq(OrganizationContext.require())),
            pageable,
            Pagination.newestFirst(RANK_CATEGORIES.CREATED_AT, RANK_CATEGORIES.ID),
            record -> toRankCategory(record.into(RANK_CATEGORIES)));
    }

    public void delete(UUID id) {
        dsl.deleteFrom(RANK_CATEGORIES)
            .where(RANK_CATEGORIES.ID.eq(id))
            .and(RANK_CATEGORIES.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    public static List<Field<?>> fields(String alias) {
        return Arrays.asList(RANK_CATEGORIES.as(alias).fields());
    }

    public static SelectJoinStep<Record> join(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final RankCategories rankCategory = RANK_CATEGORIES.as(alias);
        return select.join(rankCategory).on(rankCategory.ID.eq(foreignKey));
    }

    public static SelectJoinStep<Record> leftJoin(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final RankCategories rankCategory = RANK_CATEGORIES.as(alias);
        return select.leftJoin(rankCategory).on(rankCategory.ID.eq(foreignKey));
    }

    public static RankCategory toRankCategory(Record record, String alias) {
        return toRankCategory(record.into(RANK_CATEGORIES.as(alias)));
    }

    private static RankCategory toRankCategory(RankCategoriesRecord rankCategory) {
        return RankCategory.builder()
            .id(rankCategory.getId())
            .name(rankCategory.getName())
            .createdAt(rankCategory.getCreatedAt())
            .updatedAt(rankCategory.getUpdatedAt())
            .organizationId(rankCategory.getOrganizationId())
            .build();
    }
}
