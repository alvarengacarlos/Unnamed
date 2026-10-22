package com.alvarengacarlos.unnamed.app.member;

import static com.alvarengacarlos.unnamed.app.common.Joins.nested;
import static com.alvarengacarlos.unnamed.app.jooq.Tables.MEMBERS;
import static org.jooq.impl.DSL.noCondition;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Repository;

import com.alvarengacarlos.unnamed.app.OrganizationContext;
import com.alvarengacarlos.unnamed.app.common.Joins;
import com.alvarengacarlos.unnamed.app.common.Page;
import com.alvarengacarlos.unnamed.app.common.Pageable;
import com.alvarengacarlos.unnamed.app.common.Pagination;
import com.alvarengacarlos.unnamed.app.jooq.tables.Members;
import com.alvarengacarlos.unnamed.app.jooq.tables.records.MembersRecord;
import com.alvarengacarlos.unnamed.app.rank.RankRepository;

@Repository
public class MemberRepository {
    private static final String ALIAS = "m";
    private static final String RANK = "r";
    private final DSLContext dsl;

    public MemberRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void save(Member member) {
        dsl.insertInto(MEMBERS)
            .set(MEMBERS.ID, member.getId())
            .set(MEMBERS.NAME, member.getName())
            .set(MEMBERS.PHONE, member.getPhone())
            .set(MEMBERS.CPF, member.getCpf())
            .set(MEMBERS.ADDRESS, member.getAddress())
            .set(MEMBERS.GENDER, member.getGender())
            .set(MEMBERS.WEIGHT, member.getWeight())
            .set(MEMBERS.HEIGHT, member.getHeight())
            .set(MEMBERS.BIRTH_DATE, member.getBirthDate())
            .set(MEMBERS.IS_PWD, member.getIsPwd())
            .set(MEMBERS.IS_COMPETITOR, member.getIsCompetitor())
            .set(MEMBERS.CREATED_AT, member.getCreatedAt())
            .set(MEMBERS.UPDATED_AT, member.getUpdatedAt())
            .set(MEMBERS.RANK_ID, member.getRank().getId())
            .set(MEMBERS.ORGANIZATION_ID, OrganizationContext.require())
            .execute();
    }

    public void update(Member member) {
        dsl.update(MEMBERS)
            .set(MEMBERS.NAME, member.getName())
            .set(MEMBERS.PHONE, member.getPhone())
            .set(MEMBERS.CPF, member.getCpf())
            .set(MEMBERS.ADDRESS, member.getAddress())
            .set(MEMBERS.GENDER, member.getGender())
            .set(MEMBERS.WEIGHT, member.getWeight())
            .set(MEMBERS.HEIGHT, member.getHeight())
            .set(MEMBERS.BIRTH_DATE, member.getBirthDate())
            .set(MEMBERS.IS_PWD, member.getIsPwd())
            .set(MEMBERS.IS_COMPETITOR, member.getIsCompetitor())
            .set(MEMBERS.UPDATED_AT, member.getUpdatedAt())
            .set(MEMBERS.RANK_ID, member.getRank().getId())
            .where(MEMBERS.ID.eq(member.getId()))
            .and(MEMBERS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    public List<Member> read(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        final Members member = MEMBERS.as(ALIAS);
        return select()
            .where(member.ID.in(ids))
            .and(member.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetch(record -> toMember(record, ALIAS));
    }

    public Optional<Member> read(UUID id) {
        final Members member = MEMBERS.as(ALIAS);
        return select()
            .where(member.ID.eq(id))
            .and(member.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetchOptional(record -> toMember(record, ALIAS));
    }

    public boolean existsByCpf(String cpf, Optional<UUID> excludingId) {
        return dsl.fetchExists(
            MEMBERS,
            MEMBERS.CPF.eq(cpf)
                .and(excludingId.map(MEMBERS.ID::ne).orElse(noCondition()))
                .and(MEMBERS.ORGANIZATION_ID.eq(OrganizationContext.require())));
    }

    public Page<Member> read(Pageable pageable, MemberInput.GetMembers filterInput) {
        final Members member = MEMBERS.as(ALIAS);
        return Pagination.page(
            select().where(
                member.ORGANIZATION_ID.eq(OrganizationContext.require()),
                search(ALIAS, filterInput.name(), filterInput.cpf())),
            pageable,
            Pagination.newestFirst(member.CREATED_AT, member.ID),
            record -> toMember(record, ALIAS));
    }

    public List<Member> read(MemberInput.GetPossibleCompetitors input) {
        final Members member = MEMBERS.as(ALIAS);
        Condition condition = member.IS_COMPETITOR.eq(true)
            .and(member.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .and(input.gender().map(member.GENDER::eq).orElse(noCondition()))
            .and(input.isPwd().map(member.IS_PWD::eq).orElse(noCondition()))
            .and(input.rankId().map(member.RANK_ID::eq).orElse(noCondition()));
        if (isWeightPresent(input)) {
            condition = condition.and(member.WEIGHT.between(input.minWeight().get(), input.maxWeight().get()));
        }

        if (isHeightPresent(input)) {
            condition = condition.and(member.HEIGHT.between(input.minHeight().get(), input.maxHeight().get()));
        }

        if (isBirthdatePresent(input)) {
            condition = condition.and(member.BIRTH_DATE
                .between(input.minBirthdate().get(), input.maxBirthdate().get()));
        }

        return select()
            .where(condition)
            .fetch(record -> toMember(record, ALIAS));
    }

    private Boolean isWeightPresent(MemberInput.GetPossibleCompetitors input) {
        return input.minWeight().isPresent() && input.maxWeight().isPresent();
    }

    private Boolean isHeightPresent(MemberInput.GetPossibleCompetitors input) {
        return input.minHeight().isPresent() && input.maxHeight().isPresent();
    }

    private Boolean isBirthdatePresent(MemberInput.GetPossibleCompetitors input) {
        return input.minBirthdate().isPresent() && input.maxBirthdate().isPresent();
    }

    // The members whose name contains the text given, ignoring the case, and whose CPF is the one given
    public static Condition search(String alias, Optional<String> name, Optional<String> cpf) {
        final Members member = MEMBERS.as(alias);
        return name.map(member.NAME::containsIgnoreCase).orElse(noCondition())
            .and(cpf.map(member.CPF::eq).orElse(noCondition()));
    }

    public void delete(UUID id) {
        dsl.deleteFrom(MEMBERS)
            .where(MEMBERS.ID.eq(id))
            .and(MEMBERS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private SelectJoinStep<Record> select() {
        return joinRelations(dsl.select(fields(ALIAS)).from(MEMBERS.as(ALIAS)), ALIAS);
    }

    public static List<Field<?>> fields(String alias) {
        return Joins.concat(
            Arrays.asList(MEMBERS.as(alias).fields()),
            RankRepository.fields(nested(alias, RANK)));
    }

    public static SelectJoinStep<Record> join(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final Members member = MEMBERS.as(alias);
        return joinRelations(select.join(member).on(member.ID.eq(foreignKey)), alias);
    }

    private static SelectJoinStep<Record> joinRelations(SelectJoinStep<Record> select, String alias) {
        return RankRepository.join(select, nested(alias, RANK), MEMBERS.as(alias).RANK_ID);
    }

    public static Member toMember(Record record, String alias) {
        final MembersRecord member = record.into(MEMBERS.as(alias));
        return Member.builder()
            .id(member.getId())
            .name(member.getName())
            .phone(member.getPhone())
            .cpf(member.getCpf())
            .address(member.getAddress())
            .gender(member.getGender())
            .weight(member.getWeight())
            .height(member.getHeight())
            .birthDate(member.getBirthDate())
            .isPwd(member.getIsPwd())
            .isCompetitor(member.getIsCompetitor())
            .createdAt(member.getCreatedAt())
            .updatedAt(member.getUpdatedAt())
            .rank(RankRepository.toRank(record, nested(alias, RANK)))
            .organizationId(member.getOrganizationId())
            .build();
    }
}
