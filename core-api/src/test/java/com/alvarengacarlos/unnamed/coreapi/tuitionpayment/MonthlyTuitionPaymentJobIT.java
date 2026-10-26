package com.alvarengacarlos.unnamed.coreapi.tuitionpayment;

import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.TUITION_PAYMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alvarengacarlos.unnamed.coreapi.jooq.tables.records.TuitionPaymentsRecord;
import com.alvarengacarlos.unnamed.coreapi.studentenrollment.StudentEnrollment;
import com.alvarengacarlos.unnamed.coreapi.studentenrollment.StudentEnrollmentRepository;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.coreapi.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScoped;

@IntegrationTest
public class MonthlyTuitionPaymentJobIT implements OrganizationScoped {
    private static final YearMonth NOVEMBER_2026 = YearMonth.of(2026, Month.NOVEMBER);
    private static final short YEAR_2026 = 2026;
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TuitionPaymentService tuitionPaymentService;

    @Autowired
    private TuitionPaymentRepository tuitionPaymentRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private MonthlyTuitionPaymentJob monthlyTuitionPaymentJob;

    @Autowired
    private DSLContext dsl;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldAddAnUnpaidPaymentOfTheMonthToTheActiveEnrollmentsOfEveryOrganization() {
        final StudentEnrollment activeEnrollment = createEnrollment(true);
        final StudentEnrollment inactiveEnrollment = createEnrollment(false);
        final StudentEnrollment enrollmentOfAnotherOrganization = OrganizationScope.call(
            anotherOrganizationId,
            () -> createEnrollment(true));

        final int created = tuitionPaymentService.createMonthlyTuitionPayments(NOVEMBER_2026);

        assertThat(created).isEqualTo(2);
        assertUnpaidPaymentOfNovember2026(activeEnrollment, organizationId);
        assertUnpaidPaymentOfNovember2026(enrollmentOfAnotherOrganization, anotherOrganizationId);
        assertThat(paymentsOf(inactiveEnrollment)).isEmpty();
    }

    @Test
    void shouldKeepThePaymentOfTheMonthThatAnEnrollmentAlreadyHas() {
        final StudentEnrollment studentEnrollment = createEnrollment(true);
        final TuitionPayment paidPayment = fixtures.newTuitionPayment(studentEnrollment)
            .toBuilder()
            .month(Month.NOVEMBER)
            .year(YEAR_2026)
            .isPaid(true)
            .build();
        tuitionPaymentRepository.save(paidPayment);

        final int created = tuitionPaymentService.createMonthlyTuitionPayments(NOVEMBER_2026);

        assertThat(created).isZero();
        assertThat(paymentsOf(studentEnrollment))
            .singleElement()
            .satisfies(payment -> {
                assertThat(payment.getId()).isEqualTo(paidPayment.getId());
                assertThat(payment.getIsPaid()).isTrue();
            });
    }

    @Test
    void shouldAddTheMonthWhenTheEnrollmentHasOnlyOtherMonths() {
        final StudentEnrollment studentEnrollment = createEnrollment(true);
        tuitionPaymentRepository.save(fixtures.newTuitionPayment(studentEnrollment)
            .toBuilder()
            .month(Month.OCTOBER)
            .year(YEAR_2026)
            .isPaid(true)
            .build());

        tuitionPaymentService.createMonthlyTuitionPayments(NOVEMBER_2026);

        assertThat(paymentsOf(studentEnrollment))
            .extracting(TuitionPaymentsRecord::getMonth, TuitionPaymentsRecord::getIsPaid)
            .containsExactlyInAnyOrder(
                tuple(Month.OCTOBER, true),
                tuple(Month.NOVEMBER, false));
    }

    @Test
    void shouldAddNothingWhenItRunsAgainInTheSameMonth() {
        final StudentEnrollment studentEnrollment = createEnrollment(true);
        tuitionPaymentService.createMonthlyTuitionPayments(NOVEMBER_2026);

        final int created = tuitionPaymentService.createMonthlyTuitionPayments(NOVEMBER_2026);

        assertThat(created).isZero();
        assertThat(paymentsOf(studentEnrollment)).hasSize(1);
    }

    @Test
    void shouldAddThePaymentOfTheCurrentMonthOfTheTimeZoneOfTheServer() {
        final StudentEnrollment studentEnrollment = createEnrollment(true);

        monthlyTuitionPaymentJob.run();

        final YearMonth currentMonth = YearMonth.now();
        assertThat(paymentsOf(studentEnrollment))
            .singleElement()
            .satisfies(payment -> {
                assertThat(payment.getMonth()).isEqualTo(currentMonth.getMonth());
                assertThat(payment.getYear()).isEqualTo((short) currentMonth.getYear());
                assertThat(payment.getIsPaid()).isFalse();
            });
    }

    private StudentEnrollment createEnrollment(boolean isActive) {
        final StudentEnrollment studentEnrollment = fixtures.newStudentEnrollment(
                fixtures.createTuition(),
                fixtures.createStudent(),
                fixtures.createClazz())
            .toBuilder()
            .isActive(isActive)
            .build();
        studentEnrollmentRepository.save(studentEnrollment);
        return studentEnrollment;
    }

    private void assertUnpaidPaymentOfNovember2026(StudentEnrollment studentEnrollment, UUID expectedOrganizationId) {
        assertThat(paymentsOf(studentEnrollment))
            .singleElement()
            .satisfies(payment -> {
                assertThat(payment.getMonth()).isEqualTo(Month.NOVEMBER);
                assertThat(payment.getYear()).isEqualTo(YEAR_2026);
                assertThat(payment.getIsPaid()).isFalse();
                assertThat(payment.getOrganizationId()).isEqualTo(expectedOrganizationId);
            });
    }

    // Straight from the table: the repository only reads the organization in context.
    private List<TuitionPaymentsRecord> paymentsOf(StudentEnrollment studentEnrollment) {
        return dsl.selectFrom(TUITION_PAYMENTS)
            .where(TUITION_PAYMENTS.STUDENT_ENROLLMENT_ID.eq(studentEnrollment.getId()))
            .fetch();
    }
}
