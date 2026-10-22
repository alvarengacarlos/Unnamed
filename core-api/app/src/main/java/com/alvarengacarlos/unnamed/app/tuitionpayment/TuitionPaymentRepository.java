package com.alvarengacarlos.unnamed.app.tuitionpayment;

import static com.alvarengacarlos.unnamed.app.common.Joins.nested;
import static com.alvarengacarlos.unnamed.app.jooq.Tables.STUDENT_ENROLLMENTS;
import static com.alvarengacarlos.unnamed.app.jooq.Tables.TUITION_PAYMENTS;
import static org.jooq.impl.DSL.noCondition;

import java.time.Instant;
import java.time.Month;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

import com.alvarengacarlos.unnamed.app.OrganizationContext;
import com.alvarengacarlos.unnamed.app.common.Joins;
import com.alvarengacarlos.unnamed.app.common.Page;
import com.alvarengacarlos.unnamed.app.common.Pageable;
import com.alvarengacarlos.unnamed.app.common.Pagination;
import com.alvarengacarlos.unnamed.app.jooq.tables.StudentEnrollments;
import com.alvarengacarlos.unnamed.app.jooq.tables.TuitionPayments;
import com.alvarengacarlos.unnamed.app.jooq.tables.records.TuitionPaymentsRecord;
import com.alvarengacarlos.unnamed.app.studentenrollment.StudentEnrollmentRepository;

@Repository
public class TuitionPaymentRepository {
    private static final String ALIAS = "tp";
    private static final String STUDENT_ENROLLMENT = "se";
    private final DSLContext dsl;

    public TuitionPaymentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Adds an unpaid tuition payment of the month to each active enrollment of every organization, in one statement.
     * An enrollment that already has the month keeps it as it is. Returns how many were added.
     * It is the only query here that is not limited to the organization in context: it runs for all of them.
     */
    public int createUnpaidForActiveEnrollments(YearMonth yearMonth, Instant now) {
        return dsl.insertInto(
                TUITION_PAYMENTS,
                TUITION_PAYMENTS.ID,
                TUITION_PAYMENTS.MONTH,
                TUITION_PAYMENTS.YEAR,
                TUITION_PAYMENTS.IS_PAID,
                TUITION_PAYMENTS.CREATED_AT,
                TUITION_PAYMENTS.UPDATED_AT,
                TUITION_PAYMENTS.STUDENT_ENROLLMENT_ID,
                TUITION_PAYMENTS.ORGANIZATION_ID)
            .select(dsl.select(
                    DSL.field("gen_random_uuid()", SQLDataType.UUID),
                    DSL.val(yearMonth.getMonth(), TUITION_PAYMENTS.MONTH),
                    DSL.val((short) yearMonth.getYear(), TUITION_PAYMENTS.YEAR),
                    DSL.val(false, TUITION_PAYMENTS.IS_PAID),
                    DSL.val(now, TUITION_PAYMENTS.CREATED_AT),
                    DSL.val(now, TUITION_PAYMENTS.UPDATED_AT),
                    STUDENT_ENROLLMENTS.ID,
                    STUDENT_ENROLLMENTS.ORGANIZATION_ID)
                .from(STUDENT_ENROLLMENTS)
                .where(STUDENT_ENROLLMENTS.IS_ACTIVE.isTrue()))
            // uq_tuition_payments_student_enrollment_id_month_year: a month that exists is skipped.
            .onConflictDoNothing()
            .execute();
    }

    public void save(TuitionPayment tuitionPayment) {
        dsl.insertInto(TUITION_PAYMENTS)
            .set(TUITION_PAYMENTS.ID, tuitionPayment.getId())
            .set(TUITION_PAYMENTS.MONTH, tuitionPayment.getMonth())
            .set(TUITION_PAYMENTS.YEAR, tuitionPayment.getYear())
            .set(TUITION_PAYMENTS.IS_PAID, tuitionPayment.getIsPaid())
            .set(TUITION_PAYMENTS.CREATED_AT, tuitionPayment.getCreatedAt())
            .set(TUITION_PAYMENTS.UPDATED_AT, tuitionPayment.getUpdatedAt())
            .set(TUITION_PAYMENTS.STUDENT_ENROLLMENT_ID, tuitionPayment.getStudentEnrollment().getId())
            .set(TUITION_PAYMENTS.ORGANIZATION_ID, OrganizationContext.require())
            .execute();
    }

    public void update(TuitionPayment tuitionPayment) {
        dsl.update(TUITION_PAYMENTS)
            .set(TUITION_PAYMENTS.MONTH, tuitionPayment.getMonth())
            .set(TUITION_PAYMENTS.YEAR, tuitionPayment.getYear())
            .set(TUITION_PAYMENTS.IS_PAID, tuitionPayment.getIsPaid())
            .set(TUITION_PAYMENTS.UPDATED_AT, tuitionPayment.getUpdatedAt())
            .where(TUITION_PAYMENTS.ID.eq(tuitionPayment.getId()))
            .and(TUITION_PAYMENTS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    public Optional<TuitionPayment> read(UUID id) {
        final TuitionPayments tuitionPayment = TUITION_PAYMENTS.as(ALIAS);
        return select()
            .where(tuitionPayment.ID.eq(id))
            .and(tuitionPayment.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .fetchOptional(this::toTuitionPayment);
    }

    public boolean exists(UUID studentEnrollmentId, Month month, Short year, Optional<UUID> excludingId) {
        return dsl.fetchExists(
            TUITION_PAYMENTS,
            TUITION_PAYMENTS.STUDENT_ENROLLMENT_ID.eq(studentEnrollmentId)
                .and(TUITION_PAYMENTS.MONTH.eq(month))
                .and(TUITION_PAYMENTS.YEAR.eq(year))
                .and(excludingId.map(TUITION_PAYMENTS.ID::ne).orElse(noCondition()))
                .and(TUITION_PAYMENTS.ORGANIZATION_ID.eq(OrganizationContext.require())));
    }

    public Page<TuitionPayment> read(Pageable pageable, TuitionPaymentInput.GetTuitionPayments filterInput) {
        final TuitionPayments tuitionPayment = TUITION_PAYMENTS.as(ALIAS);
        final StudentEnrollments studentEnrollment = STUDENT_ENROLLMENTS.as(nested(ALIAS, STUDENT_ENROLLMENT));
        return Pagination.page(
            select().where(
                tuitionPayment.ORGANIZATION_ID.eq(OrganizationContext.require()),
                filterInput.studentId().map(studentEnrollment.STUDENT_ID::eq).orElse(noCondition())),
            pageable,
            Pagination.newestFirst(tuitionPayment.CREATED_AT, tuitionPayment.ID),
            this::toTuitionPayment);
    }

    public void delete(UUID id) {
        dsl.deleteFrom(TUITION_PAYMENTS)
            .where(TUITION_PAYMENTS.ID.eq(id))
            .and(TUITION_PAYMENTS.ORGANIZATION_ID.eq(OrganizationContext.require()))
            .execute();
    }

    private SelectJoinStep<Record> select() {
        final TuitionPayments tuitionPayment = TUITION_PAYMENTS.as(ALIAS);
        return StudentEnrollmentRepository.join(
            dsl.select(Joins.concat(
                    Arrays.asList(tuitionPayment.fields()),
                    StudentEnrollmentRepository.fields(nested(ALIAS, STUDENT_ENROLLMENT))))
                .from(tuitionPayment),
            nested(ALIAS, STUDENT_ENROLLMENT),
            tuitionPayment.STUDENT_ENROLLMENT_ID);
    }

    private TuitionPayment toTuitionPayment(Record record) {
        final TuitionPaymentsRecord tuitionPayment = record.into(TUITION_PAYMENTS.as(ALIAS));
        return TuitionPayment.builder()
            .id(tuitionPayment.getId())
            .month(tuitionPayment.getMonth())
            .year(tuitionPayment.getYear())
            .isPaid(tuitionPayment.getIsPaid())
            .createdAt(tuitionPayment.getCreatedAt())
            .updatedAt(tuitionPayment.getUpdatedAt())
            .studentEnrollment(StudentEnrollmentRepository.toStudentEnrollment(
                record, 
                nested(ALIAS, STUDENT_ENROLLMENT)))
            .organizationId(tuitionPayment.getOrganizationId())
            .build();
    }
}
