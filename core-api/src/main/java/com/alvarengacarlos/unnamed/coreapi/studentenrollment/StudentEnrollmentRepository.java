package com.alvarengacarlos.unnamed.coreapi.studentenrollment;

import static com.alvarengacarlos.unnamed.coreapi.common.Joins.nested;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.STUDENT_ENROLLMENTS;
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
import com.alvarengacarlos.unnamed.coreapi.clazz.ClazzRepository;
import com.alvarengacarlos.unnamed.coreapi.common.Joins;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.common.Pagination;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.StudentEnrollments;
import com.alvarengacarlos.unnamed.coreapi.jooq.tables.records.StudentEnrollmentsRecord;
import com.alvarengacarlos.unnamed.coreapi.student.StudentRepository;
import com.alvarengacarlos.unnamed.coreapi.tuition.TuitionRepository;

@Repository
public class StudentEnrollmentRepository {
    private static final String ALIAS = "se";
    private static final String TUITION = "tu";
    private static final String STUDENT = "s";
    private static final String CLAZZ = "cl";
    private final DSLContext dsl;

    public StudentEnrollmentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void save(StudentEnrollment studentEnrollment) {
        dsl.insertInto(STUDENT_ENROLLMENTS)
            .set(STUDENT_ENROLLMENTS.ID, studentEnrollment.getId())
            .set(STUDENT_ENROLLMENTS.IS_ACTIVE, studentEnrollment.getIsActive())
            .set(STUDENT_ENROLLMENTS.TUITION_ID, studentEnrollment.getTuition().getId())
            .set(STUDENT_ENROLLMENTS.STUDENT_ID, studentEnrollment.getStudent().getId())
            .set(STUDENT_ENROLLMENTS.CLAZZ_ID, studentEnrollment.getClazz().getId())
            .set(STUDENT_ENROLLMENTS.CREATED_AT, studentEnrollment.getCreatedAt())
            .set(STUDENT_ENROLLMENTS.UPDATED_AT, studentEnrollment.getUpdatedAt())
            .set(STUDENT_ENROLLMENTS.ORGANIZATION_ID, OrganizationContext.require())
            .execute();
    }

    public void update(StudentEnrollment studentEnrollment) {
        dsl.update(STUDENT_ENROLLMENTS)
            .set(STUDENT_ENROLLMENTS.IS_ACTIVE, studentEnrollment.getIsActive())
            .set(STUDENT_ENROLLMENTS.TUITION_ID, studentEnrollment.getTuition().getId())
            .set(STUDENT_ENROLLMENTS.UPDATED_AT, studentEnrollment.getUpdatedAt())
            .where(STUDENT_ENROLLMENTS.ID.eq(studentEnrollment.getId()))
            .and(STUDENT_ENROLLMENTS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    public Optional<StudentEnrollment> read(UUID id) {
        final StudentEnrollments studentEnrollment = STUDENT_ENROLLMENTS.as(ALIAS);
        return select()
            .where(studentEnrollment.ID.eq(id))
            .and(studentEnrollment.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetchOptional(record -> toStudentEnrollment(record, ALIAS));
    }

    public Page<StudentEnrollment> read(
        Pageable pageable,
        StudentEnrollmentInput.GetStudentEnrollments filterInput) {
        final StudentEnrollments studentEnrollment = STUDENT_ENROLLMENTS.as(ALIAS);
        return Pagination.page(
            select().where(
                studentEnrollment.ORGANIZATION_ID.eq(OrganizationContext.require()),
                filterInput.studentId().map(studentEnrollment.STUDENT_ID::eq).orElse(noCondition()),
                filterInput.clazzId().map(studentEnrollment.CLAZZ_ID::eq).orElse(noCondition())),
            pageable,
            Pagination.newestFirst(studentEnrollment.CREATED_AT, studentEnrollment.ID),
            record -> toStudentEnrollment(record, ALIAS));
    }

    public boolean exists(UUID studentId, UUID clazzId) {
        return dsl.fetchExists(
            STUDENT_ENROLLMENTS,
            STUDENT_ENROLLMENTS.STUDENT_ID.eq(studentId)
                .and(STUDENT_ENROLLMENTS.CLAZZ_ID.eq(clazzId))
                .and(STUDENT_ENROLLMENTS.ORGANIZATION_ID.eq(OrganizationContext.require())));
    }

    public void delete(UUID id) {
        dsl.deleteFrom(STUDENT_ENROLLMENTS)
            .where(STUDENT_ENROLLMENTS.ID.eq(id))
            .and(STUDENT_ENROLLMENTS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private SelectJoinStep<Record> select() {
        return joinRelations(dsl.select(fields(ALIAS)).from(STUDENT_ENROLLMENTS.as(ALIAS)), ALIAS);
    }

    public static List<Field<?>> fields(String alias) {
        return Joins.concat(
            Arrays.asList(STUDENT_ENROLLMENTS.as(alias).fields()),
            TuitionRepository.fields(nested(alias, TUITION)),
            StudentRepository.fields(nested(alias, STUDENT)),
            ClazzRepository.fields(nested(alias, CLAZZ)));
    }

    public static SelectJoinStep<Record> join(
        SelectJoinStep<Record> select,
        String alias,
        Field<UUID> foreignKey) {
        final StudentEnrollments studentEnrollment = STUDENT_ENROLLMENTS.as(alias);
        return joinRelations(select.join(studentEnrollment).on(studentEnrollment.ID.eq(foreignKey)), alias);
    }

    private static SelectJoinStep<Record> joinRelations(SelectJoinStep<Record> select, String alias) {
        final StudentEnrollments studentEnrollment = STUDENT_ENROLLMENTS.as(alias);
        final SelectJoinStep<Record> withTuition = TuitionRepository.join(
            select,
            nested(alias, TUITION),
            studentEnrollment.TUITION_ID);
        final SelectJoinStep<Record> withStudent = StudentRepository.join(
            withTuition,
            nested(alias, STUDENT),
            studentEnrollment.STUDENT_ID);
        return ClazzRepository.join(withStudent, nested(alias, CLAZZ), studentEnrollment.CLAZZ_ID);
    }

    public static StudentEnrollment toStudentEnrollment(Record record, String alias) {
        final StudentEnrollmentsRecord studentEnrollment = record.into(STUDENT_ENROLLMENTS.as(alias));
        return StudentEnrollment.builder()
            .id(studentEnrollment.getId())
            .isActive(studentEnrollment.getIsActive())
            .tuition(TuitionRepository.toTuition(record, nested(alias, TUITION)))
            .student(StudentRepository.toStudent(record, nested(alias, STUDENT)))
            .clazz(ClazzRepository.toClazz(record, nested(alias, CLAZZ)))
            .createdAt(studentEnrollment.getCreatedAt())
            .updatedAt(studentEnrollment.getUpdatedAt())
            .organizationId(studentEnrollment.getOrganizationId())
            .build();
    }
}
