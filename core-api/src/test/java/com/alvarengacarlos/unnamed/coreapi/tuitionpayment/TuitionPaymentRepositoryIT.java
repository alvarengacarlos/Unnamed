package com.alvarengacarlos.unnamed.coreapi.tuitionpayment;

import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameEntity;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.coreapi.testsupport.IntegrationTest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScoped;

@IntegrationTest
public class TuitionPaymentRepositoryIT implements OrganizationScoped {
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final TuitionPaymentInput.GetTuitionPayments noFilter =
        new TuitionPaymentInput.GetTuitionPayments(Optional.empty());

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TuitionPaymentRepository tuitionPaymentRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldSaveATuitionPayment() {
        final TuitionPayment tuitionPayment = fixtures.newTuitionPayment(fixtures.createStudentEnrollment());

        tuitionPaymentRepository.save(tuitionPayment);

        assertSameEntity(tuitionPaymentRepository.read(tuitionPayment.getId()).orElseThrow(), tuitionPayment);
    }

    @Test
    void shouldUpdateATuitionPayment() {
        final TuitionPayment tuitionPayment = fixtures.createTuitionPayment();
        final TuitionPayment updatedTuitionPayment = tuitionPayment.toBuilder()
            .month(tuitionPayment.getMonth().plus(1))
            .year((short) (tuitionPayment.getYear() + 1))
            .isPaid(!tuitionPayment.getIsPaid())
            .build();

        tuitionPaymentRepository.update(updatedTuitionPayment);

        assertSameEntity(tuitionPaymentRepository.read(tuitionPayment.getId()).orElseThrow(), updatedTuitionPayment);
    }

    @Test
    void shouldNotReadATuitionPaymentThatDoesNotExist() {
        assertThat(tuitionPaymentRepository.read(UUID.randomUUID())).isEmpty();
    }

    @Nested
    class Exists {
        private TuitionPayment tuitionPayment;

        @BeforeEach
        void beforeEach() {
            tuitionPayment = fixtures.createTuitionPayment();
        }

        @Test
        void shouldFindAPaymentOfTheEnrollmentInTheMonthAndYear() {
            assertThat(exists(tuitionPayment.getMonth(), tuitionPayment.getYear(), Optional.empty())).isTrue();
        }

        @Test
        void shouldNotFindAPaymentInAnotherMonth() {
            assertThat(exists(tuitionPayment.getMonth().plus(1), tuitionPayment.getYear(), Optional.empty())).isFalse();
        }

        @Test
        void shouldNotFindAPaymentInAnotherYear() {
            assertThat(exists(
                tuitionPayment.getMonth(),
                (short) (tuitionPayment.getYear() + 1),
                Optional.empty())).isFalse();
        }

        @Test
        void shouldIgnoreTheExcludedPayment() {
            assertThat(exists(
                tuitionPayment.getMonth(),
                tuitionPayment.getYear(),
                Optional.of(tuitionPayment.getId()))).isFalse();
        }

        private boolean exists(Month month, Short year, Optional<UUID> excludingId) {
            return tuitionPaymentRepository.exists(
                tuitionPayment.getStudentEnrollment().getId(),
                month,
                year,
                excludingId);
        }
    }

    @Nested
    class ReadMany {
        @Test
        void shouldReadNone() {
            final Page<TuitionPayment> page = tuitionPaymentRepository.read(new Pageable(0, 1), noFilter);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }

        @Test
        void shouldReadAll() {
            final List<TuitionPayment> tuitionPayments = List.of(
                fixtures.createTuitionPayment(),
                fixtures.createTuitionPayment(),
                fixtures.createTuitionPayment());

            final Page<TuitionPayment> page = tuitionPaymentRepository.read(new Pageable(0, 10), noFilter);

            assertSameEntity(page.content(), tuitionPayments);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadTheLastPage() {
            fixtures.createTuitionPayment();
            fixtures.createTuitionPayment();
            fixtures.createTuitionPayment();

            final Page<TuitionPayment> page = tuitionPaymentRepository.read(new Pageable(1, 2), noFilter);

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(3);
        }

        @Test
        void shouldReadOnlyThePaymentsOfTheStudent() {
            final TuitionPayment tuitionPayment = fixtures.createTuitionPayment();
            fixtures.createTuitionPayment();

            final Page<TuitionPayment> page = tuitionPaymentRepository.read(
                new Pageable(0, 10),
                new TuitionPaymentInput.GetTuitionPayments(
                    Optional.of(tuitionPayment.getStudentEnrollment().getStudent().getId())));

            assertSameEntity(page.content(), List.of(tuitionPayment));
            assertThat(page.totalElements()).isEqualTo(1);
        }
    }

    @Test
    void shouldDeleteATuitionPayment() {
        final TuitionPayment tuitionPayment = fixtures.createTuitionPayment();

        tuitionPaymentRepository.delete(tuitionPayment.getId());

        assertThat(tuitionPaymentRepository.read(tuitionPayment.getId())).isEmpty();
    }

    @Nested
    class WhenTheTuitionPaymentBelongsToAnotherOrganization {
        private TuitionPayment tuitionPayment;

        @BeforeEach
        void beforeEach() {
            tuitionPayment = OrganizationScope.call(anotherOrganizationId, fixtures::createTuitionPayment);
        }

        @Test
        void shouldNotReadIt() {
            assertThat(tuitionPaymentRepository.read(tuitionPayment.getId())).isEmpty();
            assertThat(tuitionPaymentRepository.exists(
                tuitionPayment.getStudentEnrollment().getId(),
                tuitionPayment.getMonth(),
                tuitionPayment.getYear(),
                Optional.empty())).isFalse();
            assertThat(tuitionPaymentRepository.read(new Pageable(0, 1), noFilter).totalElements()).isZero();
        }

        @Test
        void shouldNotUpdateIt() {
            tuitionPaymentRepository.update(tuitionPayment.toBuilder().isPaid(!tuitionPayment.getIsPaid()).build());

            assertSameEntity(readInAnotherOrganization(), tuitionPayment);
        }

        @Test
        void shouldNotDeleteIt() {
            tuitionPaymentRepository.delete(tuitionPayment.getId());

            assertSameEntity(readInAnotherOrganization(), tuitionPayment);
        }

        private TuitionPayment readInAnotherOrganization() {
            return OrganizationScope.call(
                anotherOrganizationId,
                () -> tuitionPaymentRepository.read(tuitionPayment.getId()).orElseThrow());
        }
    }
}
