package com.alvarengacarlos.unnamed.coreapi.grading;

import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.GRADINGS;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.common.Pagination;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.Gradings;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.records.GradingsRecord;
import com.alvarengacarlos.unnamed.coreapi.rank.Rank;
import com.alvarengacarlos.unnamed.coreapi.rank.RankNotFoundException;
import com.alvarengacarlos.unnamed.coreapi.rank.RankRepository;

@Repository
public class GradingRepository {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final DSLContext dsl;
    private final RankRepository rankRepository;

    public GradingRepository(DSLContext dsl, RankRepository rankRepository) {
        this.dsl = dsl;
        this.rankRepository = rankRepository;
    }

    public void save(Grading grading) {
        dsl.insertInto(GRADINGS)
            .set(GRADINGS.ID, grading.getId())
            .set(GRADINGS.NAME, grading.getName())
            .set(GRADINGS.WHEN, grading.getWhen())
            .set(GRADINGS.WHERE, grading.getWhere())
            .set(GRADINGS.REGISTRATION_PERIOD_START, grading.getRegistrationPeriodStart())
            .set(GRADINGS.REGISTRATION_PERIOD_END, grading.getRegistrationPeriodEnd())
            .set(GRADINGS.GRADING_PRICES, toJson(grading.getGradingPrices()))
            .set(GRADINGS.CREATED_AT, grading.getCreatedAt())
            .set(GRADINGS.UPDATED_AT, grading.getUpdatedAt())
            .set(GRADINGS.ORGANIZATION_ID, OrganizationContext.require())
            .execute();
    }

    public void update(Grading grading) {
        dsl.update(GRADINGS)
            .set(GRADINGS.NAME, grading.getName())
            .set(GRADINGS.WHEN, grading.getWhen())
            .set(GRADINGS.WHERE, grading.getWhere())
            .set(GRADINGS.REGISTRATION_PERIOD_START, grading.getRegistrationPeriodStart())
            .set(GRADINGS.REGISTRATION_PERIOD_END, grading.getRegistrationPeriodEnd())
            .set(GRADINGS.GRADING_PRICES, toJson(grading.getGradingPrices()))
            .set(GRADINGS.UPDATED_AT, grading.getUpdatedAt())
            .where(GRADINGS.ID.eq(grading.getId()))
            .and(GRADINGS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private JSONB toJson(Set<GradingPrice> gradingPrices) {
        final Set<GradingPriceJson> gradingPricesJson = gradingPrices.stream()
            .map(gradingPrice -> new GradingPriceJson(gradingPrice.getPrice(), gradingPrice.getRank().getId()))
            .collect(Collectors.toSet());
        return JSONB.valueOf(OBJECT_MAPPER.writeValueAsString(gradingPricesJson));
    }

    public Optional<Grading> read(UUID id) {
        final List<GradingsRecord> gradings = dsl.selectFrom(GRADINGS)
            .where(GRADINGS.ID.eq(id))
            .and(GRADINGS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetch();

        return toGradings(gradings).stream().findFirst();
    }

    public Page<Grading> read(Pageable pageable) {
        final Page<GradingsRecord> page = Pagination.page(
            dsl.select(GRADINGS.fields())
                .from(GRADINGS)
                .where(GRADINGS.ORGANIZATION_ID.eq(OrganizationContext.require())),
            pageable,
            Pagination.newestFirst(GRADINGS.CREATED_AT, GRADINGS.ID),
            record -> record.into(GRADINGS));

        return page.withContent(toGradings(page.content()));
    }

    public List<Grading> toGradings(List<GradingsRecord> gradings) {
        final Map<UUID, Set<GradingPriceJson>> gradingPrices = gradings.stream()
            .collect(Collectors.toMap(GradingsRecord::getId, GradingRepository::toGradingPricesJson));
        final Set<UUID> rankIds = gradingPrices.values()
            .stream()
            .flatMap(Set::stream)
            .map(GradingPriceJson::rankId)
            .collect(Collectors.toSet());
        final Map<UUID, Rank> ranks = rankRepository.read(rankIds)
            .stream()
            .collect(Collectors.toMap(Rank::getId, Function.identity()));

        return gradings.stream()
            .map(grading -> toGrading(grading, toGradingPrices(gradingPrices.get(grading.getId()), ranks)))
            .toList();
    }

    private static Set<GradingPriceJson> toGradingPricesJson(GradingsRecord grading) {
        return OBJECT_MAPPER.readValue(
            grading.getGradingPrices().data(),
            new TypeReference<Set<GradingPriceJson>>() {});
    }

    private static Set<GradingPrice> toGradingPrices(Set<GradingPriceJson> gradingPrices, Map<UUID, Rank> ranks) {
        return gradingPrices.stream()
            .map(gradingPrice -> new GradingPrice.Builder()
                .price(gradingPrice.price())
                .rank(Optional.ofNullable(ranks.get(gradingPrice.rankId()))
                    .orElseThrow(() -> new RankNotFoundException()))
                .build())
            .collect(Collectors.toSet());
    }

    public void delete(UUID id) {
        dsl.deleteFrom(GRADINGS)
            .where(GRADINGS.ID.eq(id))
            .and(GRADINGS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    public static List<Field<?>> fields(String alias) {
        return Arrays.asList(GRADINGS.as(alias).fields());
    }

    public static SelectJoinStep<Record> join(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final Gradings grading = GRADINGS.as(alias);
        return select.join(grading).on(grading.ID.eq(foreignKey));
    }

    public static GradingsRecord toGradingsRecord(Record record, String alias) {
        return record.into(GRADINGS.as(alias));
    }

    private static Grading toGrading(GradingsRecord grading, Set<GradingPrice> gradingPrices) {
        return Grading.builder()
            .id(grading.getId())
            .name(grading.getName())
            .when(grading.getWhen())
            .where(grading.getWhere())
            .registrationPeriodStart(grading.getRegistrationPeriodStart())
            .registrationPeriodEnd(grading.getRegistrationPeriodEnd())
            .gradingPrices(gradingPrices)
            .createdAt(grading.getCreatedAt())
            .updatedAt(grading.getUpdatedAt())
            .organizationId(grading.getOrganizationId())
            .build();
    }

    private static record GradingPriceJson(BigDecimal price, UUID rankId) {
    }
}
