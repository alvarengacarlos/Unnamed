package com.alvarengacarlos.unnamed.app.tuitionpayment;

import static com.alvarengacarlos.unnamed.app.testsupport.ApiClient.assertLocation;
import static com.alvarengacarlos.unnamed.app.testsupport.ApiErrors.assertFieldErrors;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertCreatedOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertUpdatedOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Month;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

import com.alvarengacarlos.unnamed.app.common.CommonOutput;
import com.alvarengacarlos.unnamed.app.studentenrollment.StudentEnrollment;
import com.alvarengacarlos.unnamed.app.studentenrollment.StudentEnrollmentOutput;
import com.alvarengacarlos.unnamed.app.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.app.testsupport.E2ETest;
import com.alvarengacarlos.unnamed.app.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScoped;
import com.alvarengacarlos.unnamed.app.testsupport.Pages;
import com.alvarengacarlos.unnamed.app.testsupport.Problem;
import com.alvarengacarlos.unnamed.app.testsupport.ValidationProblem;

@E2ETest
public class TuitionPaymentApiIT implements OrganizationScoped {
    private static final String PATH = "/tuition-payments";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final short YEAR = 2026;
    private static final Problem NOT_FOUND = Problem.of("tuition-payment-not-found", "Tuition payment not found", 404);
    private static final Problem EXISTS = Problem.of("tuition-payment-exists", "Tuition payment exists", 409);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<TuitionPaymentOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final TuitionPaymentInput.Update updateInput = new TuitionPaymentInput.Update(Month.MARCH, YEAR, true);
    private StudentEnrollment studentEnrollment;

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TuitionPaymentRepository tuitionPaymentRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @BeforeEach
    void beforeEach() {
        studentEnrollment = fixtures.createStudentEnrollment();
    }

    @Nested
    class Create {
        @Test
        void shouldCreateATuitionPayment() {
            final TuitionPaymentInput.Create input = new TuitionPaymentInput.Create(
                Month.FEBRUARY,
                YEAR,
                false,
                studentEnrollment.getId());

            final TuitionPaymentOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(input)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(TuitionPaymentOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, new TuitionPaymentOutput(
                null,
                input.month(),
                input.year(),
                input.isPaid(),
                null,
                null,
                StudentEnrollmentOutput.from(studentEnrollment),
                organizationId));
            assertSameOutput(
                TuitionPaymentOutput.from(tuitionPaymentRepository.read(output.id()).orElseThrow()),
                output);
        }

        @Test
        void shouldReturnBadRequestWhenTheInputIsInvalid() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new TuitionPaymentInput.Create(null, null, null, null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, "month", "year", "isPaid", "studentEnrollmentId");
        }

        @Test
        void shouldReturnNotFoundWhenTheStudentEnrollmentBelongsToAnotherOrganization() {
            final StudentEnrollment studentEnrollmentOfAnotherOrganization = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createStudentEnrollment);

            api.client(organizationId).post()
                .uri(PATH)
                .body(new TuitionPaymentInput.Create(
                    Month.FEBRUARY,
                    YEAR,
                    false,
                    studentEnrollmentOfAnotherOrganization.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("student-enrollment-not-found", "Student enrollment not found", 404));
        }

        @Test
        void shouldReturnConflictWhenTheMonthIsRegistered() {
            final TuitionPayment tuitionPayment = fixtures.createTuitionPayment(studentEnrollment);

            api.client(organizationId).post()
                .uri(PATH)
                .body(new TuitionPaymentInput.Create(
                    tuitionPayment.getMonth(),
                    tuitionPayment.getYear(),
                    true,
                    studentEnrollment.getId()))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(EXISTS);
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheTuitionPaymentsOfTheOrganization() {
            final TuitionPayment tuitionPayment = fixtures.createTuitionPayment(studentEnrollment);
            OrganizationScope.run(anotherOrganizationId, fixtures::createTuitionPayment);

            final CommonOutput.PaginationOutput<TuitionPaymentOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(TuitionPaymentOutput.from(tuitionPayment)), 10));
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetATuitionPayment() {
            final TuitionPayment tuitionPayment = fixtures.createTuitionPayment(studentEnrollment);

            final TuitionPaymentOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, tuitionPayment.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TuitionPaymentOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, TuitionPaymentOutput.from(tuitionPayment));
        }

        @Test
        void shouldReturnNotFoundWhenItDoesNotExist() {
            api.client(organizationId).get()
                .uri(PATH_WITH_ID, UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldReturnNotFoundWhenItBelongsToAnotherOrganization() {
            final TuitionPayment tuitionPayment = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createTuitionPayment);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, tuitionPayment.getId())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }
    }

    @Nested
    class Update {
        @Test
        void shouldUpdateATuitionPayment() {
            final TuitionPayment tuitionPayment = fixtures.createTuitionPayment(studentEnrollment);

            final TuitionPaymentOutput output = api.client(organizationId).put()
                .uri(PATH_WITH_ID, tuitionPayment.getId())
                .body(updateInput)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TuitionPaymentOutput.class)
                .returnResult()
                .getResponseBody();

            assertUpdatedOutput(output, TuitionPaymentOutput.from(tuitionPayment.toBuilder()
                .month(updateInput.month())
                .year(updateInput.year())
                .isPaid(updateInput.isPaid())
                .build()));
            assertSameOutput(
                TuitionPaymentOutput.from(tuitionPaymentRepository.read(tuitionPayment.getId()).orElseThrow()),
                output);
        }

        @Test
        void shouldMarkTheSameMonthAsPaid() {
            final TuitionPayment tuitionPayment = fixtures.createTuitionPayment(studentEnrollment);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, tuitionPayment.getId())
                .body(new TuitionPaymentInput.Update(tuitionPayment.getMonth(), tuitionPayment.getYear(), true))
                .exchange()
                .expectStatus()
                .isOk();
        }

        @Test
        void shouldReturnConflictWhenTheMonthBelongsToAnotherPayment() {
            final TuitionPayment tuitionPayment = fixtures.createTuitionPayment(studentEnrollment);
            final TuitionPayment anotherTuitionPayment = fixtures.createTuitionPayment(studentEnrollment);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, tuitionPayment.getId())
                .body(new TuitionPaymentInput.Update(
                    anotherTuitionPayment.getMonth(),
                    anotherTuitionPayment.getYear(),
                    true))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(EXISTS);
        }

        @Test
        void shouldReturnNotFoundWhenItDoesNotExist() {
            api.client(organizationId).put()
                .uri(PATH_WITH_ID, UUID.randomUUID())
                .body(updateInput)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldNotUpdateATuitionPaymentOfAnotherOrganization() {
            final TuitionPayment tuitionPayment = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createTuitionPayment);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, tuitionPayment.getId())
                .body(updateInput)
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(tuitionPayment.getId()), tuitionPayment);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteATuitionPayment() {
            final TuitionPayment tuitionPayment = fixtures.createTuitionPayment(studentEnrollment);

            final TuitionPaymentOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tuitionPayment.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TuitionPaymentOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, TuitionPaymentOutput.from(tuitionPayment));
            assertThat(tuitionPaymentRepository.read(tuitionPayment.getId())).isEmpty();
        }

        @Test
        void shouldReturnNotFoundWhenItDoesNotExist() {
            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldNotDeleteATuitionPaymentOfAnotherOrganization() {
            final TuitionPayment tuitionPayment = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createTuitionPayment);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tuitionPayment.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(tuitionPayment.getId()), tuitionPayment);
        }
    }

    private TuitionPayment readInAnotherOrganization(UUID id) {
        return OrganizationScope.call(anotherOrganizationId, () -> tuitionPaymentRepository.read(id).orElseThrow());
    }
}
