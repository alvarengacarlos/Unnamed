package com.alvarengacarlos.unnamed.coreapi.student;

import static com.alvarengacarlos.unnamed.coreapi.common.Joins.nested;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.STUDENTS;
import static org.jooq.impl.DSL.noCondition;

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
import com.alvarengacarlos.unnamed.coreapi.common.Joins;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.common.Pagination;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.Students;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.records.StudentsRecord;
import com.alvarengacarlos.unnamed.coreapi.member.MemberRepository;

@Repository
public class StudentRepository {
    private static final String ALIAS = "s";
    private static final String MEMBER = "m";
    private final DSLContext dsl;

    public StudentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void save(Student student) {
        dsl.insertInto(STUDENTS)
            .set(STUDENTS.ID, student.getId())
            .set(STUDENTS.MEMBER_ID, student.getMember().getId())
            .set(STUDENTS.CREATED_AT, student.getCreatedAt())
            .set(STUDENTS.UPDATED_AT, student.getUpdatedAt())
            .set(STUDENTS.ORGANIZATION_ID, OrganizationContext.require())
            .execute();
    }

    public Optional<Student> read(UUID id) {
        final Students student = STUDENTS.as(ALIAS);
        return select()
            .where(student.ID.eq(id))
            .and(student.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetchOptional(record -> toStudent(record, ALIAS));
    }

    public boolean existsByMemberId(UUID memberId) {
        return dsl.fetchExists(
            STUDENTS,
            STUDENTS.MEMBER_ID.eq(memberId)
                .and(STUDENTS.ORGANIZATION_ID.eq(OrganizationContext.require())));
    }

    public Page<Student> read(
        Pageable pageable, 
        StudentInput.GetStudents filterInput) {
        final Students student = STUDENTS.as(ALIAS);
        return Pagination.page(
            select().where(
                student.ORGANIZATION_ID.eq(OrganizationContext.require()),
                filterInput.memberId().map(student.MEMBER_ID::eq).orElse(noCondition()),
                MemberRepository.search(nested(ALIAS, MEMBER), filterInput.name(), filterInput.cpf())),
            pageable,
            Pagination.newestFirst(student.CREATED_AT, student.ID),
            record -> toStudent(record, ALIAS));
    }

    public void delete(UUID id) {
        dsl.deleteFrom(STUDENTS)
            .where(STUDENTS.ID.eq(id))
            .and(STUDENTS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private SelectJoinStep<Record> select() {
        return joinRelations(dsl.select(fields(ALIAS)).from(STUDENTS.as(ALIAS)), ALIAS);
    }

    public static List<Field<?>> fields(String alias) {
        return Joins.concat(
            Arrays.asList(STUDENTS.as(alias).fields()),
            MemberRepository.fields(nested(alias, MEMBER)));
    }

    public static SelectJoinStep<Record> join(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final Students student = STUDENTS.as(alias);
        return joinRelations(select.join(student).on(student.ID.eq(foreignKey)), alias);
    }

    private static SelectJoinStep<Record> joinRelations(SelectJoinStep<Record> select, String alias) {
        return MemberRepository.join(select, nested(alias, MEMBER), STUDENTS.as(alias).MEMBER_ID);
    }

    public static Student toStudent(Record record, String alias) {
        final StudentsRecord student = record.into(STUDENTS.as(alias));
        return Student.builder()
            .id(student.getId())
            .member(MemberRepository.toMember(record, nested(alias, MEMBER)))
            .createdAt(student.getCreatedAt())
            .updatedAt(student.getUpdatedAt())
            .organizationId(student.getOrganizationId())
            .build();
    }
}
