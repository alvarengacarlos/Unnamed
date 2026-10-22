package com.alvarengacarlos.unnamed.app.tournamentcompetitor;

import static com.alvarengacarlos.unnamed.app.common.Joins.nested;
import static com.alvarengacarlos.unnamed.app.jooq.Tables.TOURNAMENT_COMPETITORS;
import static org.jooq.impl.DSL.noCondition;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Row2;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.alvarengacarlos.unnamed.app.OrganizationContext;
import com.alvarengacarlos.unnamed.app.common.Joins;
import com.alvarengacarlos.unnamed.app.common.Page;
import com.alvarengacarlos.unnamed.app.common.Pageable;
import com.alvarengacarlos.unnamed.app.common.Pagination;
import com.alvarengacarlos.unnamed.app.jooq.tables.TournamentCompetitors;
import com.alvarengacarlos.unnamed.app.jooq.tables.records.TournamentCompetitorsRecord;
import com.alvarengacarlos.unnamed.app.member.MemberRepository;
import com.alvarengacarlos.unnamed.app.tournament.TournamentRepository;

@Repository
public class TournamentCompetitorRepository {
    private static final String ALIAS = "tc";
    private static final String TOURNAMENT = "to";
    private static final String MEMBER = "m";
    private final DSLContext dsl;

    public TournamentCompetitorRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void save(TournamentCompetitor tournamentCompetitor) {
        dsl.insertInto(TOURNAMENT_COMPETITORS)
            .set(toRecord(tournamentCompetitor))
            .execute();
    }

    public void saveInBulk(List<TournamentCompetitor> tournamentCompetitors) {
        dsl.batchInsert(tournamentCompetitors.stream()
                .map(this::toRecord)
                .toList())
            .execute();
    }

    private TournamentCompetitorsRecord toRecord(TournamentCompetitor tournamentCompetitor) {
        final TournamentCompetitorsRecord record = dsl.newRecord(TOURNAMENT_COMPETITORS);
        record.setId(tournamentCompetitor.getId());
        record.setPaymentMethod(tournamentCompetitor.getPaymentMethod());
        record.setIsPaid(tournamentCompetitor.getIsPaid());
        record.setPoints(tournamentCompetitor.getPoints());
        record.setCreatedAt(tournamentCompetitor.getCreatedAt());
        record.setUpdatedAt(tournamentCompetitor.getUpdatedAt());
        record.setTournamentId(tournamentCompetitor.getTournament().getId());
        record.setMemberId(tournamentCompetitor.getMember().getId());
        record.setOrganizationId(OrganizationContext.require());
        return record;
    }

    public void update(TournamentCompetitor tournamentCompetitor) {
        dsl.update(TOURNAMENT_COMPETITORS)
            .set(TOURNAMENT_COMPETITORS.PAYMENT_METHOD, tournamentCompetitor.getPaymentMethod())
            .set(TOURNAMENT_COMPETITORS.IS_PAID, tournamentCompetitor.getIsPaid())
            .set(TOURNAMENT_COMPETITORS.POINTS, tournamentCompetitor.getPoints())
            .set(TOURNAMENT_COMPETITORS.UPDATED_AT, tournamentCompetitor.getUpdatedAt())
            .set(TOURNAMENT_COMPETITORS.TOURNAMENT_ID, tournamentCompetitor.getTournament().getId())
            .set(TOURNAMENT_COMPETITORS.MEMBER_ID, tournamentCompetitor.getMember().getId())
            .where(TOURNAMENT_COMPETITORS.ID.eq(tournamentCompetitor.getId()))
            .and(TOURNAMENT_COMPETITORS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    public Optional<TournamentCompetitor> read(UUID id) {
        final TournamentCompetitors tournamentCompetitor = TOURNAMENT_COMPETITORS.as(ALIAS);
        return select()
            .where(tournamentCompetitor.ID.eq(id))
            .and(tournamentCompetitor.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetchOptional(record -> toTournamentCompetitor(record, ALIAS));
    }

    public List<TournamentCompetitor> read(Set<UUID> ids) {
        final TournamentCompetitors tournamentCompetitor = TOURNAMENT_COMPETITORS.as(ALIAS);
        return select()
            .where(tournamentCompetitor.ID.in(ids))
            .and(tournamentCompetitor.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetch(record -> toTournamentCompetitor(record, ALIAS));
    }

    public boolean exists(Collection<TournamentCompetitor> tournamentCompetitors) {
        final List<Row2<UUID, UUID>> tournamentAndMemberIds = tournamentCompetitors.stream()
            .map(tc -> DSL.row(tc.getTournament().getId(), tc.getMember().getId()))
            .toList();
        return dsl.fetchExists(
            TOURNAMENT_COMPETITORS,
            DSL.row(TOURNAMENT_COMPETITORS.TOURNAMENT_ID, TOURNAMENT_COMPETITORS.MEMBER_ID)
                .in(tournamentAndMemberIds)
                .and(TOURNAMENT_COMPETITORS.ORGANIZATION_ID.eq(OrganizationContext.require())));
    }

    public boolean exists(UUID tournamentId, UUID memberId) {
        return dsl.fetchExists(
            TOURNAMENT_COMPETITORS,
            TOURNAMENT_COMPETITORS.TOURNAMENT_ID.eq(tournamentId)
                .and(TOURNAMENT_COMPETITORS.MEMBER_ID.eq(memberId))
                .and(TOURNAMENT_COMPETITORS.ORGANIZATION_ID.eq(OrganizationContext.require())));
    }

    public Page<TournamentCompetitor> read(
        Pageable pageable,
        TournamentCompetitorInput.GetTournamentCompetitors filterInput) {
        final TournamentCompetitors tournamentCompetitor = TOURNAMENT_COMPETITORS.as(ALIAS);
        return Pagination.page(
            select().where(
                tournamentCompetitor.ORGANIZATION_ID.eq(OrganizationContext.require()),
                filterInput.tournamentId().map(tournamentCompetitor.TOURNAMENT_ID::eq).orElse(noCondition())),
            pageable,
            Pagination.newestFirst(tournamentCompetitor.CREATED_AT, tournamentCompetitor.ID),
            record -> toTournamentCompetitor(record, ALIAS));
    }

    public void delete(UUID id) {
        dsl.deleteFrom(TOURNAMENT_COMPETITORS)
            .where(TOURNAMENT_COMPETITORS.ID.eq(id))
            .and(TOURNAMENT_COMPETITORS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private SelectJoinStep<Record> select() {
        return joinRelations(dsl.select(fields(ALIAS)).from(TOURNAMENT_COMPETITORS.as(ALIAS)), ALIAS);
    }

    public static List<Field<?>> fields(String alias) {
        return Joins.concat(
            Arrays.asList(TOURNAMENT_COMPETITORS.as(alias).fields()),
            TournamentRepository.fields(nested(alias, TOURNAMENT)),
            MemberRepository.fields(nested(alias, MEMBER)));
    }

    public static SelectJoinStep<Record> join(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final TournamentCompetitors tournamentCompetitor = TOURNAMENT_COMPETITORS.as(alias);
        return joinRelations(select.join(tournamentCompetitor).on(tournamentCompetitor.ID.eq(foreignKey)), alias);
    }

    private static SelectJoinStep<Record> joinRelations(SelectJoinStep<Record> select, String alias) {
        final TournamentCompetitors tournamentCompetitor = TOURNAMENT_COMPETITORS.as(alias);
        final SelectJoinStep<Record> withTournament = TournamentRepository.join(
            select,
            nested(alias, TOURNAMENT),
            tournamentCompetitor.TOURNAMENT_ID);
        return MemberRepository.join(withTournament, nested(alias, MEMBER), tournamentCompetitor.MEMBER_ID);
    }

    public static TournamentCompetitor toTournamentCompetitor(Record record, String alias) {
        final TournamentCompetitorsRecord tournamentCompetitor = record.into(TOURNAMENT_COMPETITORS.as(alias));
        return TournamentCompetitor.builder()
            .id(tournamentCompetitor.getId())
            .paymentMethod(tournamentCompetitor.getPaymentMethod())
            .isPaid(tournamentCompetitor.getIsPaid())
            .points(tournamentCompetitor.getPoints())
            .createdAt(tournamentCompetitor.getCreatedAt())
            .updatedAt(tournamentCompetitor.getUpdatedAt())
            .tournament(TournamentRepository.toTournament(record, nested(alias, TOURNAMENT)))
            .member(MemberRepository.toMember(record, nested(alias, MEMBER)))
            .organizationId(tournamentCompetitor.getOrganizationId())
            .build();
    }
}
