package com.alvarengacarlos.unnamed.app.teacher;

import static com.alvarengacarlos.unnamed.app.common.Joins.nested;
import static com.alvarengacarlos.unnamed.app.jooq.Tables.TEACHERS;

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
import com.alvarengacarlos.unnamed.app.common.Joins;
import com.alvarengacarlos.unnamed.app.common.Page;
import com.alvarengacarlos.unnamed.app.common.Pageable;
import com.alvarengacarlos.unnamed.app.common.Pagination;
import com.alvarengacarlos.unnamed.app.jooq.tables.Teachers;
import com.alvarengacarlos.unnamed.app.jooq.tables.records.TeachersRecord;
import com.alvarengacarlos.unnamed.app.member.MemberRepository;

@Repository
public class TeacherRepository {
    private static final String ALIAS = "t";
    private static final String MEMBER = "m";
    private final DSLContext dsl;

    public TeacherRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void save(Teacher teacher) {
        dsl.insertInto(TEACHERS)
            .set(TEACHERS.ID, teacher.getId())
            .set(TEACHERS.MEMBER_ID, teacher.getMember().getId())
            .set(TEACHERS.CREATED_AT, teacher.getCreatedAt())
            .set(TEACHERS.UPDATED_AT, teacher.getUpdatedAt())
            .set(TEACHERS.ORGANIZATION_ID, OrganizationContext.require())
            .execute();
    }

    public Optional<Teacher> read(UUID id) {
        final Teachers teacher = TEACHERS.as(ALIAS);
        return select()
            .where(teacher.ID.eq(id))
            .and(teacher.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetchOptional(record -> toTeacher(record, ALIAS));
    }

    public boolean existsByMemberId(UUID memberId) {
        return dsl.fetchExists(
            TEACHERS,
            TEACHERS.MEMBER_ID.eq(memberId)
                .and(TEACHERS.ORGANIZATION_ID.eq(OrganizationContext.require())));
    }

    public Page<Teacher> read(Pageable pageable, TeacherInput.GetTeachers filterInput) {
        final Teachers teacher = TEACHERS.as(ALIAS);
        return Pagination.page(
            select().where(
                teacher.ORGANIZATION_ID.eq(OrganizationContext.require()),
                MemberRepository.search(nested(ALIAS, MEMBER), filterInput.name(), filterInput.cpf())),
            pageable,
            Pagination.newestFirst(teacher.CREATED_AT, teacher.ID),
            record -> toTeacher(record, ALIAS));
    }

    public void delete(UUID id) {
        dsl.deleteFrom(TEACHERS)
            .where(TEACHERS.ID.eq(id))
            .and(TEACHERS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private SelectJoinStep<Record> select() {
        return joinRelations(dsl.select(fields(ALIAS)).from(TEACHERS.as(ALIAS)), ALIAS);
    }

    public static List<Field<?>> fields(String alias) {
        return Joins.concat(
            Arrays.asList(TEACHERS.as(alias).fields()),
            MemberRepository.fields(nested(alias, MEMBER)));
    }

    public static SelectJoinStep<Record> join(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final Teachers teacher = TEACHERS.as(alias);
        return joinRelations(select.join(teacher).on(teacher.ID.eq(foreignKey)), alias);
    }

    private static SelectJoinStep<Record> joinRelations(SelectJoinStep<Record> select, String alias) {
        return MemberRepository.join(select, nested(alias, MEMBER), TEACHERS.as(alias).MEMBER_ID);
    }

    public static Teacher toTeacher(Record record, String alias) {
        final TeachersRecord teacher = record.into(TEACHERS.as(alias));
        return Teacher.builder()
            .id(teacher.getId())
            .member(MemberRepository.toMember(record, nested(alias, MEMBER)))
            .createdAt(teacher.getCreatedAt())
            .updatedAt(teacher.getUpdatedAt())
            .organizationId(teacher.getOrganizationId())
            .build();
    }
}
