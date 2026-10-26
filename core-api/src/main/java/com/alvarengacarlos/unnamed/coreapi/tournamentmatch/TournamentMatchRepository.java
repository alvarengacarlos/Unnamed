package com.alvarengacarlos.unnamed.coreapi.tournamentmatch;

import static com.alvarengacarlos.unnamed.coreapi.common.Joins.nested;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.TOURNAMENT_MATCHES;
import static org.jooq.impl.DSL.noCondition;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;
import com.alvarengacarlos.unnamed.coreapi.common.Joins;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.common.Pagination;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.TournamentMatches;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.records.TournamentMatchesRecord;
import com.alvarengacarlos.unnamed.coreapi.tournament.TournamentRepository;
import com.alvarengacarlos.unnamed.coreapi.tournamentcompetitor.TournamentCompetitorRepository;

@Repository
public class TournamentMatchRepository {
    private static final String ALIAS = "tm";
    private static final String COMPETITOR_A = "ca";
    private static final String COMPETITOR_B = "cb";
    private static final String TOURNAMENT = "to";
    private final DSLContext dsl;

    public TournamentMatchRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void save(TournamentMatch tournamentMatch) {
        dsl.insertInto(TOURNAMENT_MATCHES)
            .set(toRecord(tournamentMatch))
            .execute();
    }

    public void saveInBulk(List<TournamentMatch> tournamentMatches) {
        dsl.batchInsert(tournamentMatches.stream()
                .map(this::toRecord)
                .toList())
            .execute();
    }

    private TournamentMatchesRecord toRecord(TournamentMatch tournamentMatch) {
        final TournamentMatchesRecord record = dsl.newRecord(TOURNAMENT_MATCHES);
        record.setId(tournamentMatch.getId());
        record.setWhen(tournamentMatch.getWhen());
        record.setWhere(tournamentMatch.getWhere());
        record.setResult(tournamentMatch.getResult());
        record.setCreatedAt(tournamentMatch.getCreatedAt());
        record.setUpdatedAt(tournamentMatch.getUpdatedAt());
        record.setCompetitorAId(tournamentMatch.getCompetitorA().getId());
        record.setCompetitorBId(tournamentMatch.getCompetitorB().getId());
        record.setTournamentId(tournamentMatch.getTournament().getId());
        record.setOrganizationId(OrganizationContext.require());
        return record;
    }

    public void update(TournamentMatch tournamentMatch) {
        dsl.update(TOURNAMENT_MATCHES)
            .set(TOURNAMENT_MATCHES.WHEN, tournamentMatch.getWhen())
            .set(TOURNAMENT_MATCHES.WHERE, tournamentMatch.getWhere())
            .set(TOURNAMENT_MATCHES.RESULT, tournamentMatch.getResult())
            .set(TOURNAMENT_MATCHES.UPDATED_AT, tournamentMatch.getUpdatedAt())
            .where(TOURNAMENT_MATCHES.ID.eq(tournamentMatch.getId()))
            .and(TOURNAMENT_MATCHES.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    public Optional<TournamentMatch> read(UUID id) {
        final TournamentMatches tournamentMatch = TOURNAMENT_MATCHES.as(ALIAS);
        return select()
            .where(tournamentMatch.ID.eq(id))
            .and(tournamentMatch.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetchOptional(this::toTournamentMatch);
    }

    /**
     * Makes the other transactions that create matches in these tournaments wait until this one ends, so the check
     * for an existing match and the insert act as one. A unique constraint cannot do it: it does not see that A x B and
     * B x A are the same match. The locks are taken in a fixed order, so two bulks cannot deadlock.
     */
    public void lockTournaments(Collection<UUID> tournamentIds) {
        tournamentIds.stream()
            .sorted()
            .forEach(tournamentId -> dsl.execute(
                "select pg_advisory_xact_lock(hashtextextended(?, 0))",
                tournamentId.toString()));
    }

    public boolean exists(
        UUID tournamentId,
        UUID tournamentCompetitorAId,
        UUID tournamentCompetitorBId) {
        return dsl.fetchExists(
            TOURNAMENT_MATCHES,
            matchCondition(tournamentId, tournamentCompetitorAId, tournamentCompetitorBId)
                .and(TOURNAMENT_MATCHES.ORGANIZATION_ID.eq(OrganizationContext.require())));
    }

    public boolean exists(List<TournamentMatch> tournamentMatches) {
        final List<Condition> matchConditions = tournamentMatches.stream()
            .map(tm -> matchCondition(
                tm.getTournament().getId(),
                tm.getCompetitorA().getId(),
                tm.getCompetitorB().getId()))
            .toList();
        return dsl.fetchExists(
            TOURNAMENT_MATCHES,
            DSL.or(matchConditions)
                .and(TOURNAMENT_MATCHES.ORGANIZATION_ID.eq(OrganizationContext.require())));
    }

    private Condition matchCondition(
        UUID tournamentId,
        UUID tournamentCompetitorAId,
        UUID tournamentCompetitorBId) {
        return TOURNAMENT_MATCHES.TOURNAMENT_ID.eq(tournamentId)
            .and(TOURNAMENT_MATCHES.COMPETITOR_A_ID.in(tournamentCompetitorAId, tournamentCompetitorBId))
            .and(TOURNAMENT_MATCHES.COMPETITOR_B_ID.in(tournamentCompetitorAId, tournamentCompetitorBId));
    }

    public Page<TournamentMatch> read(
        Pageable pageable,
        TournamentMatchInput.GetTournamentMatches filterInput) {
        final TournamentMatches tournamentMatch = TOURNAMENT_MATCHES.as(ALIAS);
        return Pagination.page(
            select().where(
                tournamentMatch.ORGANIZATION_ID.eq(OrganizationContext.require()),
                filterInput.tournamentId().map(tournamentMatch.TOURNAMENT_ID::eq).orElse(noCondition())),
            pageable,
            Pagination.newestFirst(tournamentMatch.CREATED_AT, tournamentMatch.ID),
            this::toTournamentMatch);
    }

    public void delete(UUID id) {
        dsl.deleteFrom(TOURNAMENT_MATCHES)
            .where(TOURNAMENT_MATCHES.ID.eq(id))
            .and(TOURNAMENT_MATCHES.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private SelectJoinStep<Record> select() {
        final TournamentMatches tournamentMatch = TOURNAMENT_MATCHES.as(ALIAS);
        final SelectJoinStep<Record> select = dsl.select(Joins.concat(
                Arrays.asList(tournamentMatch.fields()),
                TournamentCompetitorRepository.fields(nested(ALIAS, COMPETITOR_A)),
                TournamentCompetitorRepository.fields(nested(ALIAS, COMPETITOR_B)),
                TournamentRepository.fields(nested(ALIAS, TOURNAMENT))))
            .from(tournamentMatch);
        final SelectJoinStep<Record> withCompetitorA = TournamentCompetitorRepository.join(
            select,
            nested(ALIAS, COMPETITOR_A),
            tournamentMatch.COMPETITOR_A_ID);
        final SelectJoinStep<Record> withCompetitorB = TournamentCompetitorRepository.join(
            withCompetitorA,
            nested(ALIAS, COMPETITOR_B),
            tournamentMatch.COMPETITOR_B_ID);
        return TournamentRepository.join(withCompetitorB, nested(ALIAS, TOURNAMENT), tournamentMatch.TOURNAMENT_ID);
    }

    private TournamentMatch toTournamentMatch(Record record) {
        final TournamentMatchesRecord tournamentMatch = record.into(TOURNAMENT_MATCHES.as(ALIAS));
        return TournamentMatch.builder()
            .id(tournamentMatch.getId())
            .when(tournamentMatch.getWhen())
            .where(tournamentMatch.getWhere())
            .result(tournamentMatch.getResult())
            .createdAt(tournamentMatch.getCreatedAt())
            .updatedAt(tournamentMatch.getUpdatedAt())
            .tournamentCompetitorA(TournamentCompetitorRepository.toTournamentCompetitor(
                record, 
                nested(ALIAS, COMPETITOR_A)))
            .tournamentCompetitorB(TournamentCompetitorRepository.toTournamentCompetitor(
                record, 
                nested(ALIAS, COMPETITOR_B)))
            .tournament(TournamentRepository.toTournament(record, nested(ALIAS, TOURNAMENT)))
            .organizationId(tournamentMatch.getOrganizationId())
            .build();
    }
}
