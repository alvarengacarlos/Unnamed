package com.alvarengacarlos.unnamed.coreapi.gradingcandidate;

import static com.alvarengacarlos.unnamed.coreapi.common.Joins.nested;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.GRADING_CANDIDATES;
import static org.jooq.impl.DSL.noCondition;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Repository;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;
import com.alvarengacarlos.unnamed.coreapi.common.Joins;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.common.Pagination;
import com.alvarengacarlos.unnamed.coreapi.grading.Grading;
import com.alvarengacarlos.unnamed.coreapi.grading.GradingRepository;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.GradingCandidates;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.records.GradingCandidatesRecord;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.records.GradingsRecord;
import com.alvarengacarlos.unnamed.coreapi.member.Member;
import com.alvarengacarlos.unnamed.coreapi.member.MemberRepository;
import com.alvarengacarlos.unnamed.coreapi.rank.Rank;
import com.alvarengacarlos.unnamed.coreapi.rank.RankRepository;

@Repository
public class GradingCandidateRepository {
    private static final String ALIAS = "gc";
    private static final String RANK = "r";
    private static final String GRADING = "g";
    private static final String MEMBER = "m";
    private final DSLContext dsl;
    private final GradingRepository gradingRepository;

    public GradingCandidateRepository(DSLContext dsl, GradingRepository gradingRepository) {
        this.dsl = dsl;
        this.gradingRepository = gradingRepository;
    }

    public void save(GradingCandidate gradingCandidate) {
        dsl.insertInto(GRADING_CANDIDATES)
            .set(GRADING_CANDIDATES.ID, gradingCandidate.getId())
            .set(GRADING_CANDIDATES.PAYMENT_METHOD, gradingCandidate.getPaymentMethod())
            .set(GRADING_CANDIDATES.IS_PAID, gradingCandidate.getIsPaid())
            .set(GRADING_CANDIDATES.CREATED_AT, gradingCandidate.getCreatedAt())
            .set(GRADING_CANDIDATES.UPDATED_AT, gradingCandidate.getUpdatedAt())
            .set(GRADING_CANDIDATES.RANK_ID, gradingCandidate.getRank().getId())
            .set(GRADING_CANDIDATES.GRADING_ID, gradingCandidate.getGrading().getId())
            .set(GRADING_CANDIDATES.MEMBER_ID, gradingCandidate.getMember().getId())
            .set(GRADING_CANDIDATES.ORGANIZATION_ID, OrganizationContext.require())
            .execute();
    }

    public void update(GradingCandidate gradingCandidate) {
        dsl.update(GRADING_CANDIDATES)
            .set(GRADING_CANDIDATES.PAYMENT_METHOD, gradingCandidate.getPaymentMethod())
            .set(GRADING_CANDIDATES.IS_PAID, gradingCandidate.getIsPaid())
            .set(GRADING_CANDIDATES.UPDATED_AT, gradingCandidate.getUpdatedAt())
            .set(GRADING_CANDIDATES.RANK_ID, gradingCandidate.getRank().getId())
            .where(GRADING_CANDIDATES.ID.eq(gradingCandidate.getId()))
            .and(GRADING_CANDIDATES.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    public Optional<GradingCandidate> read(UUID id) {
        final GradingCandidates gradingCandidate = GRADING_CANDIDATES.as(ALIAS);
        final List<JoinedRow> joinedRows = select()
            .where(gradingCandidate.ID.eq(id))
            .and(gradingCandidate.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetch(GradingCandidateRepository::toJoinedRow);

        return toGradingCandidates(joinedRows).stream().findFirst();
    }

    public boolean exists(UUID gradingId, UUID memberId) {
        return dsl.fetchExists(
            GRADING_CANDIDATES,
            GRADING_CANDIDATES.GRADING_ID.eq(gradingId)
                .and(GRADING_CANDIDATES.MEMBER_ID.eq(memberId))
                .and(GRADING_CANDIDATES.ORGANIZATION_ID.eq(OrganizationContext.require())));
    }

    public Page<GradingCandidate> read(
        Pageable pageable,
        GradingCandidateInput.GetGradingCandidates filterInput) {
        final GradingCandidates gradingCandidate = GRADING_CANDIDATES.as(ALIAS);
        final Page<JoinedRow> page = Pagination.page(
            select().where(
                gradingCandidate.ORGANIZATION_ID.eq(OrganizationContext.require()),
                filterInput.gradingId().map(gradingCandidate.GRADING_ID::eq).orElse(noCondition())),
            pageable,
            Pagination.newestFirst(gradingCandidate.CREATED_AT, gradingCandidate.ID),
            GradingCandidateRepository::toJoinedRow);

        return page.withContent(toGradingCandidates(page.content()));
    }

    public void delete(UUID id) {
        dsl.deleteFrom(GRADING_CANDIDATES)
            .where(GRADING_CANDIDATES.ID.eq(id))
            .and(GRADING_CANDIDATES.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private List<GradingCandidate> toGradingCandidates(List<JoinedRow> joinedRows) {
        final List<Grading> gradings = gradingRepository.toGradings(joinedRows.stream()
            .map(JoinedRow::grading)
            .toList());

        return IntStream.range(0, joinedRows.size())
            .mapToObj(index -> toGradingCandidate(joinedRows.get(index), gradings.get(index)))
            .toList();
    }

    private static GradingCandidate toGradingCandidate(JoinedRow joinedRow, Grading grading) {
        final GradingCandidatesRecord gradingCandidate = joinedRow.gradingCandidate();
        return GradingCandidate.builder()
            .id(gradingCandidate.getId())
            .paymentMethod(gradingCandidate.getPaymentMethod())
            .isPaid(gradingCandidate.getIsPaid())
            .createdAt(gradingCandidate.getCreatedAt())
            .updatedAt(gradingCandidate.getUpdatedAt())
            .rank(joinedRow.rank())
            .grading(grading)
            .member(joinedRow.member())
            .organizationId(gradingCandidate.getOrganizationId())
            .build();
    }

    private SelectJoinStep<Record> select() {
        final GradingCandidates gradingCandidate = GRADING_CANDIDATES.as(ALIAS);
        final SelectJoinStep<Record> select = dsl.select(Joins.concat(
                Arrays.asList(gradingCandidate.fields()),
                RankRepository.fields(nested(ALIAS, RANK)),
                GradingRepository.fields(nested(ALIAS, GRADING)),
                MemberRepository.fields(nested(ALIAS, MEMBER))))
            .from(gradingCandidate);
        final SelectJoinStep<Record> withRank = RankRepository.join(
            select,
            nested(ALIAS, RANK),
            gradingCandidate.RANK_ID);
        final SelectJoinStep<Record> withGrading = GradingRepository.join(
            withRank,
            nested(ALIAS, GRADING),
            gradingCandidate.GRADING_ID);
        return MemberRepository.join(withGrading, nested(ALIAS, MEMBER), gradingCandidate.MEMBER_ID);
    }

    private static JoinedRow toJoinedRow(Record record) {
        return new JoinedRow(
            record.into(GRADING_CANDIDATES.as(ALIAS)),
            RankRepository.toRank(record, nested(ALIAS, RANK)),
            GradingRepository.toGradingsRecord(record, nested(ALIAS, GRADING)),
            MemberRepository.toMember(record, nested(ALIAS, MEMBER)));
    }

    private static record JoinedRow(
        GradingCandidatesRecord gradingCandidate,
        Rank rank,
        GradingsRecord grading,
        Member member) {
    }
}
