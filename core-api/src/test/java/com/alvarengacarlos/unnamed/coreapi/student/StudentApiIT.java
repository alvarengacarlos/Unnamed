package com.alvarengacarlos.unnamed.coreapi.student;

import static com.alvarengacarlos.unnamed.coreapi.testsupport.ApiClient.assertLocation;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.ApiErrors.assertFieldErrors;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertCreatedOutput;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameEntity;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;
import com.alvarengacarlos.unnamed.coreapi.member.Member;
import com.alvarengacarlos.unnamed.coreapi.member.MemberOutput;
import com.alvarengacarlos.unnamed.coreapi.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.coreapi.testsupport.ApiErrors;
import com.alvarengacarlos.unnamed.coreapi.testsupport.E2ETest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScoped;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Pages;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Problem;
import com.alvarengacarlos.unnamed.coreapi.testsupport.ValidationProblem;

@E2ETest
public class StudentApiIT implements OrganizationScoped {
    private static final String PATH = "/students";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final String MEMBER_ID_FIELD = "memberId";
    private static final String MEMBER_ID_FILTER = "&memberId={memberId}";
    private static final Problem NOT_FOUND = Problem.of("student-not-found", "Student not found", 404);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<StudentOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private StudentRepository studentRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Nested
    class Create {
        @Test
        void shouldCreateAStudent() {
            final Member member = fixtures.createMember();

            final StudentOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(new StudentInput.Create(member.getId()))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(StudentOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, new StudentOutput(
                null,
                null,
                null,
                MemberOutput.from(member),
                organizationId));
            assertSameOutput(StudentOutput.from(studentRepository.read(output.id()).orElseThrow()), output);
        }

        @Test
        void shouldReturnBadRequestWhenTheMemberIdIsMissing() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new StudentInput.Create(null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, MEMBER_ID_FIELD);
        }

        @Test
        void shouldReturnNotFoundWhenTheMemberBelongsToAnotherOrganization() {
            final Member member = OrganizationScope.call(anotherOrganizationId, fixtures::createMember);

            api.client(organizationId).post()
                .uri(PATH)
                .body(new StudentInput.Create(member.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("member-not-found", "Member not found", 404));
        }

        @Test
        void shouldReturnConflictWhenTheMemberIsAStudent() {
            final Student student = fixtures.createStudent();

            api.client(organizationId).post()
                .uri(PATH)
                .body(new StudentInput.Create(student.getMember().getId()))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("student-exists", "Student exists", 409));
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheStudentsOfTheOrganization() {
            final Student student = fixtures.createStudent();
            OrganizationScope.run(anotherOrganizationId, fixtures::createStudent);

            final CommonOutput.PaginationOutput<StudentOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(StudentOutput.from(student)), 10));
        }

        @Test
        void shouldFilterByMemberId() {
            final Student student = fixtures.createStudent();
            fixtures.createStudent();

            final CommonOutput.PaginationOutput<StudentOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH) + MEMBER_ID_FILTER, 0, 10, student.getMember().getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(StudentOutput.from(student)), 10));
        }

        @Test
        void shouldReturnBadRequestWhenTheMemberIdFilterIsNotAnUuid() {
            final ValidationProblem errors = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH) + MEMBER_ID_FILTER, 0, 10, "member")
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, MEMBER_ID_FIELD);
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetAStudent() {
            final Student student = fixtures.createStudent();

            final StudentOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, student.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(StudentOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, StudentOutput.from(student));
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
            final Student student = OrganizationScope.call(anotherOrganizationId, fixtures::createStudent);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, student.getId())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteAStudent() {
            final Student student = fixtures.createStudent();

            final StudentOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, student.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(StudentOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, StudentOutput.from(student));
            assertThat(studentRepository.read(student.getId())).isEmpty();
        }

        @Test
        void shouldReturnConflictWhenAnEnrollmentHasIt() {
            final Student student = fixtures.createStudent();
            fixtures.createStudentEnrollment(fixtures.createTuition(), student, fixtures.createClazz());

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, student.getId())
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.ASSOCIATED_RESOURCES);
            assertThat(studentRepository.read(student.getId())).isPresent();
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
        void shouldNotDeleteAStudentOfAnotherOrganization() {
            final Student student = OrganizationScope.call(anotherOrganizationId, fixtures::createStudent);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, student.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(
                OrganizationScope.call(
                    anotherOrganizationId,
                    () -> studentRepository.read(student.getId()).orElseThrow()),
                student);
        }
    }
}
