package com.alvarengacarlos.unnamed.coreapi.tournament;

import static com.alvarengacarlos.unnamed.coreapi.common.Joins.nested;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.TOURNAMENTS;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;
import com.alvarengacarlos.unnamed.coreapi.common.Joins;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.common.Pagination;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.Tournaments;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.records.TournamentsRecord;
import com.alvarengacarlos.unnamed.coreapi.member.Member;
import com.alvarengacarlos.unnamed.coreapi.rank.Rank;
import com.alvarengacarlos.unnamed.coreapi.rank.RankRepository;

@Repository
public class TournamentRepository {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ALIAS = "to";
    private static final String RANK = "r";
    private static final String DIVISION_CRITERION_RANK_ID = "rankId";
    private final DSLContext dsl;

    public TournamentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void save(Tournament tournament) {
        dsl.insertInto(TOURNAMENTS)
            .set(TOURNAMENTS.ID, tournament.getId())
            .set(TOURNAMENTS.NAME, tournament.getName())
            .set(TOURNAMENTS.FORMAT, tournament.getFormat())
            .set(TOURNAMENTS.DIVISION_CRITERION, toJson(tournament.getTournamentDivisionCriterion()))
            .set(TOURNAMENTS.PARTICIPATION_PRICE, tournament.getParticipationPrice())
            .set(TOURNAMENTS.START_DATE_TIME, tournament.getStartDateTime())
            .set(TOURNAMENTS.END_DATE_TIME, tournament.getEndDateTime())
            .set(TOURNAMENTS.WHERE, tournament.getWhere())
            .set(TOURNAMENTS.CREATED_AT, tournament.getCreatedAt())
            .set(TOURNAMENTS.UPDATED_AT, tournament.getUpdatedAt())
            .set(TOURNAMENTS.ORGANIZATION_ID, OrganizationContext.require())
            .execute();
    }

    public void update(Tournament tournament) {
        dsl.update(TOURNAMENTS)
            .set(TOURNAMENTS.NAME, tournament.getName())
            .set(TOURNAMENTS.FORMAT, tournament.getFormat())
            .set(TOURNAMENTS.DIVISION_CRITERION, toJson(tournament.getTournamentDivisionCriterion()))
            .set(TOURNAMENTS.PARTICIPATION_PRICE, tournament.getParticipationPrice())
            .set(TOURNAMENTS.START_DATE_TIME, tournament.getStartDateTime())
            .set(TOURNAMENTS.END_DATE_TIME, tournament.getEndDateTime())
            .set(TOURNAMENTS.WHERE, tournament.getWhere())
            .set(TOURNAMENTS.UPDATED_AT, tournament.getUpdatedAt())
            .where(TOURNAMENTS.ID.eq(tournament.getId()))
            .and(TOURNAMENTS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private JSONB toJson(TournamentDivisionCriterion tournamentDivisionCriterion) {
        return JSONB.valueOf(OBJECT_MAPPER.writeValueAsString(
            DivisionCriterionJson.from(tournamentDivisionCriterion)));
    }

    public Optional<Tournament> read(UUID id) {
        final Tournaments tournament = TOURNAMENTS.as(ALIAS);
        return select()
            .where(tournament.ID.eq(id))
            .and(tournament.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetchOptional(record -> toTournament(record, ALIAS));
    }

    public List<Tournament> read(Set<UUID> ids) {
        final Tournaments tournament = TOURNAMENTS.as(ALIAS);
        return select()
            .where(tournament.ID.in(ids))
            .and(tournament.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetch(record -> toTournament(record, ALIAS));
    }

    public Page<Tournament> read(Pageable pageable) {
        return Pagination.page(
            select().where(TOURNAMENTS.as(ALIAS).ORGANIZATION_ID.eq(OrganizationContext.require())),
            pageable,
            Pagination.newestFirst(TOURNAMENTS.as(ALIAS).CREATED_AT, TOURNAMENTS.as(ALIAS).ID),
            record -> toTournament(record, ALIAS));
    }

    public void delete(UUID id) {
        dsl.deleteFrom(TOURNAMENTS)
            .where(TOURNAMENTS.ID.eq(id))
            .and(TOURNAMENTS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private SelectJoinStep<Record> select() {
        return joinRelations(dsl.select(fields(ALIAS)).from(TOURNAMENTS.as(ALIAS)), ALIAS);
    }

    public static List<Field<?>> fields(String alias) {
        return Joins.concat(
            Arrays.asList(TOURNAMENTS.as(alias).fields()),
            RankRepository.fields(nested(alias, RANK)));
    }

    public static SelectJoinStep<Record> join(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final Tournaments tournament = TOURNAMENTS.as(alias);
        return joinRelations(select.join(tournament).on(tournament.ID.eq(foreignKey)), alias);
    }

    private static SelectJoinStep<Record> joinRelations(SelectJoinStep<Record> select, String alias) {
        final Field<UUID> divisionCriterionRankId = field(
            "({0} ->> {1})::uuid",
            SQLDataType.UUID,
            TOURNAMENTS.as(alias).DIVISION_CRITERION,
            inline(DIVISION_CRITERION_RANK_ID));
        return RankRepository.leftJoin(select, nested(alias, RANK), divisionCriterionRankId);
    }

    public static Tournament toTournament(Record record, String alias) {
        final TournamentsRecord tournament = record.into(TOURNAMENTS.as(alias));
        Optional<Rank> rank = Optional.empty();
        if (RankRepository.isPresent(record, nested(alias, RANK))) {
            rank = Optional.of(RankRepository.toRank(record, nested(alias, RANK)));
        }

        final DivisionCriterionJson divisionCriterion = OBJECT_MAPPER.readValue(
            tournament.getDivisionCriterion().data(),
            DivisionCriterionJson.class);
        return Tournament.builder()
            .id(tournament.getId())
            .name(tournament.getName())
            .format(tournament.getFormat())
            .tournamentDivisionCriterion(divisionCriterion.toTournamentDivisionCriterion(rank))
            .participationPrice(tournament.getParticipationPrice())
            .startDateTime(tournament.getStartDateTime())
            .endDateTime(tournament.getEndDateTime())
            .where(tournament.getWhere())
            .createdAt(tournament.getCreatedAt())
            .updatedAt(tournament.getUpdatedAt())
            .organizationId(tournament.getOrganizationId())
            .build();
    }

    private static record DivisionCriterionJson(
        Optional<Member.Gender> gender,
        Optional<BigDecimal> weight,
        Optional<BigDecimal> height,
        Optional<Byte> age,
        Optional<Boolean> isPwd,
        Optional<UUID> rankId) {

        static DivisionCriterionJson from(TournamentDivisionCriterion tournamentDivisionCriterion) {
            return new DivisionCriterionJson(
                tournamentDivisionCriterion.getGender(),
                tournamentDivisionCriterion.getWeight(),
                tournamentDivisionCriterion.getHeight(),
                tournamentDivisionCriterion.getAge(),
                tournamentDivisionCriterion.getIsPwd(),
                tournamentDivisionCriterion.getRank().map(Rank::getId));
        }

        TournamentDivisionCriterion toTournamentDivisionCriterion(Optional<Rank> rank) {
            return new TournamentDivisionCriterion.Builder()
                .gender(gender)
                .weight(weight)
                .height(height)
                .age(age)
                .isPwd(isPwd)
                .rank(rank)
                .build();
        }
    }
}
