package com.alvarengacarlos.unnamed.coreapi.rank;

import static com.alvarengacarlos.unnamed.coreapi.common.Joins.nested;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.RANKS;
import static org.jooq.impl.DSL.noCondition;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Repository;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;
import com.alvarengacarlos.unnamed.coreapi.common.Joins;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.common.Pagination;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.Ranks;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.records.RanksRecord;
import com.alvarengacarlos.unnamed.coreapi.rankcategory.RankCategoryRepository;

@Repository
public class RankRepository {
    private static final String ALIAS = "r";
    private static final String CATEGORY = "c";
    private final DSLContext dsl;

    public RankRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void save(Rank rank) {
        dsl.insertInto(RANKS)
            .set(RANKS.ID, rank.getId())
            .set(RANKS.NAME, rank.getName())
            .set(RANKS.ORDER, rank.getOrder().shortValue())
            .set(RANKS.RANK_CATEGORY_ID, rank.getCategory().getId())
            .set(RANKS.CREATED_AT, rank.getCreatedAt())
            .set(RANKS.UPDATED_AT, rank.getUpdatedAt())
            .set(RANKS.ORGANIZATION_ID, OrganizationContext.require())
            .execute();
    }

    public void update(Rank rank) {
        dsl.update(RANKS)
            .set(RANKS.NAME, rank.getName())
            .set(RANKS.UPDATED_AT, rank.getUpdatedAt())
            .where(RANKS.ID.eq(rank.getId()))
            .and(RANKS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    public Optional<Rank> read(UUID id) {
        final Ranks rank = RANKS.as(ALIAS);
        return select()
            .where(rank.ID.eq(id))
            .and(rank.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetchOptional(record -> toRank(record, ALIAS));
    }

    public List<Rank> read(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        final Ranks rank = RANKS.as(ALIAS);
        return select()
            .where(rank.ID.in(ids))
            .and(rank.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetch(record -> toRank(record, ALIAS));
    }

    public boolean exists(UUID rankCategoryId, Byte order) {
        return dsl.fetchExists(
            RANKS,
            RANKS.RANK_CATEGORY_ID.eq(rankCategoryId)
                .and(RANKS.ORDER.eq(order.shortValue()))
                .and(RANKS.ORGANIZATION_ID.eq(OrganizationContext.require())));
    }

    public Page<Rank> read(Pageable pageable, RankInput.GetRanks filterInput) {
        final Ranks rank = RANKS.as(ALIAS);
        return Pagination.page(
            select().where(
                rank.ORGANIZATION_ID.eq(OrganizationContext.require()),
                filterInput.categoryId().map(rank.RANK_CATEGORY_ID::eq).orElse(noCondition())),
            pageable,
            Pagination.newestFirst(rank.CREATED_AT, rank.ID),
            record -> toRank(record, ALIAS));
    }

    public void delete(UUID id) {
        dsl.deleteFrom(RANKS)
            .where(RANKS.ID.eq(id))
            .and(RANKS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private SelectJoinStep<Record> select() {
        return joinRelations(dsl.select(fields(ALIAS)).from(RANKS.as(ALIAS)), ALIAS);
    }

    public static List<Field<?>> fields(String alias) {
        return Joins.concat(
            Arrays.asList(RANKS.as(alias).fields()),
            RankCategoryRepository.fields(nested(alias, CATEGORY)));
    }

    public static SelectJoinStep<Record> join(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final Ranks rank = RANKS.as(alias);
        return joinRelations(select.join(rank).on(rank.ID.eq(foreignKey)), alias);
    }

    public static SelectJoinStep<Record> leftJoin(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final Ranks rank = RANKS.as(alias);
        return RankCategoryRepository.leftJoin(
            select.leftJoin(rank).on(rank.ID.eq(foreignKey)),
            nested(alias, CATEGORY),
            rank.RANK_CATEGORY_ID);
    }

    private static SelectJoinStep<Record> joinRelations(SelectJoinStep<Record> select, String alias) {
        return RankCategoryRepository.join(
            select,
            nested(alias, CATEGORY),
            RANKS.as(alias).RANK_CATEGORY_ID);
    }

    public static boolean isPresent(Record record, String alias) {
        return record.get(RANKS.as(alias).ID) != null;
    }

    public static Rank toRank(Record record, String alias) {
        final RanksRecord rank = record.into(RANKS.as(alias));
        return Rank.builder()
            .id(rank.getId())
            .name(rank.getName())
            .order(rank.getOrder().byteValue())
            .createdAt(rank.getCreatedAt())
            .updatedAt(rank.getUpdatedAt())
            .category(RankCategoryRepository.toRankCategory(record, nested(alias, CATEGORY)))
            .organizationId(rank.getOrganizationId())
            .build();
    }
}
