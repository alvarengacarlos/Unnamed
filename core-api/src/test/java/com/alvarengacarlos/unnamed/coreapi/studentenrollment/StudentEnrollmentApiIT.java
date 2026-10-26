package com.alvarengacarlos.unnamed.coreapi.studentenrollment;

import static com.alvarengacarlos.unnamed.coreapi.testsupport.ApiClient.assertLocation;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.ApiErrors.assertFieldErrors;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertCreatedOutput;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameEntity;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameOutput;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertUpdatedOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

import com.alvarengacarlos.unnamed.coreapi.clazz.Clazz;
import com.alvarengacarlos.unnamed.coreapi.clazz.ClazzOutput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;
import com.alvarengacarlos.unnamed.coreapi.student.Student;
import com.alvarengacarlos.unnamed.coreapi.student.StudentOutput;
import com.alvarengacarlos.unnamed.coreapi.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.coreapi.testsupport.ApiErrors;
import com.alvarengacarlos.unnamed.coreapi.testsupport.E2ETest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScoped;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Pages;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Problem;
import com.alvarengacarlos.unnamed.coreapi.testsupport.ValidationProblem;
import com.alvarengacarlos.unnamed.coreapi.tuition.Tuition;
import com.alvarengacarlos.unnamed.coreapi.tuition.TuitionOutput;

@E2ETest
public class StudentEnrollmentApiIT implements OrganizationScoped {
    private static final String PATH = "/student-enrollments";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final String TUITION_ID_FIELD = "tuitionId";
    private static final Problem NOT_FOUND = Problem.of(
        "student-enrollment-not-found",
        "Student enrollment not found",
        404);
    private static final Problem TUITION_NOT_FOUND = Problem.of("tuition-not-found", "Tuition not found", 404);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<StudentEnrollmentOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private Tuition tuition;
    private Student student;
    private Clazz clazz;

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @BeforeEach
    void beforeEach() {
        tuition = fixtures.createTuition();
        student = fixtures.createStudent();
        clazz = fixtures.createClazz();
    }

    @Nested
    class Create {
        @Test
        void shouldCreateAnActiveStudentEnrollment() {
            final StudentEnrollmentOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(new StudentEnrollmentInput.Create(tuition.getId(), student.getId(), clazz.getId()))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(StudentEnrollmentOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, new StudentEnrollmentOutput(
                null,
                true,
                TuitionOutput.from(tuition),
                StudentOutput.from(student),
                ClazzOutput.from(clazz),
                null,
                null,
                organizationId));
            assertSameOutput(
                StudentEnrollmentOutput.from(studentEnrollmentRepository.read(output.id()).orElseThrow()),
                output);
        }

        @Test
        void shouldReturnBadRequestWhenTheInputIsInvalid() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new StudentEnrollmentInput.Create(null, null, null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, TUITION_ID_FIELD, "studentId", "clazzId");
        }

        @Test
        void shouldReturnNotFoundWhenTheClazzBelongsToAnotherOrganization() {
            final Clazz clazzOfAnotherOrganization = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createClazz);

            api.client(organizationId).post()
                .uri(PATH)
                .body(new StudentEnrollmentInput.Create(
                    tuition.getId(),
                    student.getId(),
                    clazzOfAnotherOrganization.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("clazz-not-found", "Clazz not found", 404));
        }

        @Test
        void shouldReturnConflictWhenTheStudentIsEnrolledInTheClazz() {
            fixtures.createStudentEnrollment(tuition, student, clazz);

            api.client(organizationId).post()
                .uri(PATH)
                .body(new StudentEnrollmentInput.Create(tuition.getId(), student.getId(), clazz.getId()))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("student-enrollment-exists", "Student enrollment exists", 409));
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheStudentEnrollmentsOfTheOrganization() {
            final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment(tuition, student, clazz);
            OrganizationScope.run(anotherOrganizationId, fixtures::createStudentEnrollment);

            final CommonOutput.PaginationOutput<StudentEnrollmentOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(StudentEnrollmentOutput.from(studentEnrollment)), 10));
        }

        @Test
        void shouldFilterByStudentAndClazz() {
            final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment(tuition, student, clazz);
            fixtures.createStudentEnrollment(tuition, student, fixtures.createClazz());
            fixtures.createStudentEnrollment(tuition, fixtures.createStudent(), clazz);

            final CommonOutput.PaginationOutput<StudentEnrollmentOutput> page = api.client(organizationId).get()
                .uri(
                    ApiClient.paged(PATH) + "&studentId={studentId}&clazzId={clazzId}",
                    0,
                    10,
                    student.getId(),
                    clazz.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(StudentEnrollmentOutput.from(studentEnrollment)), 10));
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetAStudentEnrollment() {
            final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment(tuition, student, clazz);

            final StudentEnrollmentOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, studentEnrollment.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(StudentEnrollmentOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, StudentEnrollmentOutput.from(studentEnrollment));
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
            final StudentEnrollment studentEnrollment = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createStudentEnrollment);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, studentEnrollment.getId())
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
        void shouldDeactivateAndChangeTheTuition() {
            final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment(tuition, student, clazz);
            final Tuition newTuition = fixtures.createTuition();

            final StudentEnrollmentOutput output = api.client(organizationId).put()
                .uri(PATH_WITH_ID, studentEnrollment.getId())
                .body(new StudentEnrollmentInput.Update(false, newTuition.getId()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(StudentEnrollmentOutput.class)
                .returnResult()
                .getResponseBody();

            assertUpdatedOutput(output, StudentEnrollmentOutput.from(studentEnrollment.toBuilder()
                .isActive(false)
                .tuition(newTuition)
                .build()));
            assertSameOutput(
                StudentEnrollmentOutput.from(
                    studentEnrollmentRepository.read(studentEnrollment.getId()).orElseThrow()),
                output);
        }

        @Test
        void shouldReturnBadRequestWhenTheInputIsInvalid() {
            final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment(tuition, student, clazz);

            final ValidationProblem errors = api.client(organizationId).put()
                .uri(PATH_WITH_ID, studentEnrollment.getId())
                .body(new StudentEnrollmentInput.Update(null, null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, "isActive", TUITION_ID_FIELD);
        }

        @Test
        void shouldReturnNotFoundWhenTheTuitionDoesNotExist() {
            final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment(tuition, student, clazz);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, studentEnrollment.getId())
                .body(new StudentEnrollmentInput.Update(false, UUID.randomUUID()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(TUITION_NOT_FOUND);
        }

        @Test
        void shouldNotUpdateAStudentEnrollmentOfAnotherOrganization() {
            final StudentEnrollment studentEnrollment = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createStudentEnrollment);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, studentEnrollment.getId())
                .body(new StudentEnrollmentInput.Update(false, tuition.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);

            assertSameEntity(readInAnotherOrganization(studentEnrollment.getId()), studentEnrollment);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteAStudentEnrollment() {
            final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment(tuition, student, clazz);

            final StudentEnrollmentOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, studentEnrollment.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(StudentEnrollmentOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, StudentEnrollmentOutput.from(studentEnrollment));
            assertThat(studentEnrollmentRepository.read(studentEnrollment.getId())).isEmpty();
        }

        @Test
        void shouldReturnConflictWhenATuitionPaymentHasIt() {
            final StudentEnrollment studentEnrollment = fixtures.createStudentEnrollment(tuition, student, clazz);
            fixtures.createTuitionPayment(studentEnrollment);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, studentEnrollment.getId())
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.ASSOCIATED_RESOURCES);
            assertThat(studentEnrollmentRepository.read(studentEnrollment.getId())).isPresent();
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
        void shouldNotDeleteAStudentEnrollmentOfAnotherOrganization() {
            final StudentEnrollment studentEnrollment = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createStudentEnrollment);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, studentEnrollment.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(studentEnrollment.getId()), studentEnrollment);
        }
    }

    private StudentEnrollment readInAnotherOrganization(UUID id) {
        return OrganizationScope.call(
            anotherOrganizationId,
            () -> studentEnrollmentRepository.read(id).orElseThrow());
    }
}
