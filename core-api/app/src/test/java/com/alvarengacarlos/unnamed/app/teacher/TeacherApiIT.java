package com.alvarengacarlos.unnamed.app.teacher;

import static com.alvarengacarlos.unnamed.app.testsupport.ApiClient.assertLocation;
import static com.alvarengacarlos.unnamed.app.testsupport.ApiErrors.assertFieldErrors;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertCreatedOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

import com.alvarengacarlos.unnamed.app.common.CommonOutput;
import com.alvarengacarlos.unnamed.app.member.Member;
import com.alvarengacarlos.unnamed.app.member.MemberOutput;
import com.alvarengacarlos.unnamed.app.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.app.testsupport.ApiErrors;
import com.alvarengacarlos.unnamed.app.testsupport.E2ETest;
import com.alvarengacarlos.unnamed.app.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScoped;
import com.alvarengacarlos.unnamed.app.testsupport.Pages;
import com.alvarengacarlos.unnamed.app.testsupport.Problem;
import com.alvarengacarlos.unnamed.app.testsupport.ValidationProblem;

@E2ETest
public class TeacherApiIT implements OrganizationScoped {
    private static final String PATH = "/teachers";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final Problem NOT_FOUND = Problem.of("teacher-not-found", "Teacher not found", 404);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<TeacherOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TeacherRepository teacherRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Nested
    class Create {
        @Test
        void shouldCreateATeacher() {
            final Member member = fixtures.createMember();

            final TeacherOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(new TeacherInput.Create(member.getId()))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(TeacherOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, new TeacherOutput(
                null,
                null,
                null,
                MemberOutput.from(member),
                organizationId));
            assertSameOutput(TeacherOutput.from(teacherRepository.read(output.id()).orElseThrow()), output);
        }

        @Test
        void shouldReturnBadRequestWhenTheMemberIdIsMissing() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new TeacherInput.Create(null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, "memberId");
        }

        @Test
        void shouldReturnNotFoundWhenTheMemberBelongsToAnotherOrganization() {
            final Member member = OrganizationScope.call(anotherOrganizationId, fixtures::createMember);

            api.client(organizationId).post()
                .uri(PATH)
                .body(new TeacherInput.Create(member.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("member-not-found", "Member not found", 404));
        }

        @Test
        void shouldReturnConflictWhenTheMemberIsATeacher() {
            final Teacher teacher = fixtures.createTeacher();

            api.client(organizationId).post()
                .uri(PATH)
                .body(new TeacherInput.Create(teacher.getMember().getId()))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("teacher-exists", "Teacher exists", 409));
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheTeachersOfTheOrganization() {
            final Teacher teacher = fixtures.createTeacher();
            OrganizationScope.run(anotherOrganizationId, fixtures::createTeacher);

            final CommonOutput.PaginationOutput<TeacherOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(TeacherOutput.from(teacher)), 10));
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetATeacher() {
            final Teacher teacher = fixtures.createTeacher();

            final TeacherOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, teacher.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TeacherOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, TeacherOutput.from(teacher));
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
            final Teacher teacher = OrganizationScope.call(anotherOrganizationId, fixtures::createTeacher);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, teacher.getId())
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
        void shouldDeleteATeacher() {
            final Teacher teacher = fixtures.createTeacher();

            final TeacherOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, teacher.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TeacherOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, TeacherOutput.from(teacher));
            assertThat(teacherRepository.read(teacher.getId())).isEmpty();
        }

        @Test
        void shouldReturnConflictWhenAClassHasIt() {
            final Teacher teacher = fixtures.createTeacher();
            fixtures.createClazz(teacher);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, teacher.getId())
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.ASSOCIATED_RESOURCES);
            assertThat(teacherRepository.read(teacher.getId())).isPresent();
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
        void shouldNotDeleteATeacherOfAnotherOrganization() {
            final Teacher teacher = OrganizationScope.call(anotherOrganizationId, fixtures::createTeacher);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, teacher.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(
                OrganizationScope.call(
                    anotherOrganizationId,
                    () -> teacherRepository.read(teacher.getId()).orElseThrow()),
                teacher);
        }
    }
}
