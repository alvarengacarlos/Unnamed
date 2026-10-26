package com.alvarengacarlos.unnamed.coreapi.clazz;

import static com.alvarengacarlos.unnamed.coreapi.testsupport.ApiClient.assertLocation;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.ApiErrors.assertFieldErrors;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertCreatedOutput;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameEntity;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameOutput;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertUpdatedOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;
import com.alvarengacarlos.unnamed.coreapi.teacher.Teacher;
import com.alvarengacarlos.unnamed.coreapi.teacher.TeacherOutput;
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
public class ClazzApiIT implements OrganizationScoped {
    private static final String PATH = "/clazzes";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final Problem NOT_FOUND = Problem.of("clazz-not-found", "Clazz not found", 404);
    private static final Problem TEACHER_NOT_FOUND = Problem.of("teacher-not-found", "Teacher not found", 404);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<ClazzOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private Teacher teacher;

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private ClazzRepository clazzRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @BeforeEach
    void beforeEach() {
        teacher = fixtures.createTeacher();
    }

    @Nested
    class Create {
        @Test
        void shouldCreateAClazz() {
            final ClazzInput.CreateUpdate input = input(teacher.getId());

            final ClazzOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(input)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(ClazzOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, expectedOutput(null, input, teacher));
            assertSameOutput(ClazzOutput.from(clazzRepository.read(output.id()).orElseThrow()), output);
        }

        @Test
        void shouldReturnBadRequestWhenTheInputIsInvalid() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new ClazzInput.CreateUpdate(" ", null, null, null, Set.of(), null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, "name", "martialArt", "startTime", "endTime", "weekdays", "teacherId");
        }

        @Test
        void shouldReturnNotFoundWhenTheTeacherBelongsToAnotherOrganization() {
            final Teacher teacherOfAnotherOrganization = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createTeacher);

            api.client(organizationId).post()
                .uri(PATH)
                .body(input(teacherOfAnotherOrganization.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(TEACHER_NOT_FOUND);
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheClazzesOfTheOrganization() {
            final Clazz clazz = fixtures.createClazz(teacher);
            OrganizationScope.run(anotherOrganizationId, fixtures::createClazz);

            final CommonOutput.PaginationOutput<ClazzOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(ClazzOutput.from(clazz)), 10));
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetAClazz() {
            final Clazz clazz = fixtures.createClazz(teacher);

            final ClazzOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, clazz.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClazzOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, ClazzOutput.from(clazz));
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
            final Clazz clazz = OrganizationScope.call(anotherOrganizationId, fixtures::createClazz);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, clazz.getId())
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
        void shouldUpdateAClazz() {
            final Clazz clazz = fixtures.createClazz(teacher);
            final Teacher newTeacher = fixtures.createTeacher();
            final ClazzInput.CreateUpdate input = input(newTeacher.getId());

            final ClazzOutput output = api.client(organizationId).put()
                .uri(PATH_WITH_ID, clazz.getId())
                .body(input)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClazzOutput.class)
                .returnResult()
                .getResponseBody();

            assertUpdatedOutput(output, expectedOutput(clazz, input, newTeacher));
            assertSameOutput(ClazzOutput.from(clazzRepository.read(clazz.getId()).orElseThrow()), output);
        }

        @Test
        void shouldReturnNotFoundWhenTheTeacherDoesNotExist() {
            final Clazz clazz = fixtures.createClazz(teacher);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, clazz.getId())
                .body(input(UUID.randomUUID()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(TEACHER_NOT_FOUND);
        }

        @Test
        void shouldReturnNotFoundWhenItDoesNotExist() {
            api.client(organizationId).put()
                .uri(PATH_WITH_ID, UUID.randomUUID())
                .body(input(teacher.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldNotUpdateAClazzOfAnotherOrganization() {
            final Clazz clazz = OrganizationScope.call(anotherOrganizationId, fixtures::createClazz);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, clazz.getId())
                .body(input(teacher.getId()))
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(clazz.getId()), clazz);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteAClazz() {
            final Clazz clazz = fixtures.createClazz(teacher);

            final ClazzOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, clazz.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClazzOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, ClazzOutput.from(clazz));
            assertThat(clazzRepository.read(clazz.getId())).isEmpty();
        }

        @Test
        void shouldReturnConflictWhenAnEnrollmentHasIt() {
            final Clazz clazz = fixtures.createClazz(teacher);
            fixtures.createStudentEnrollment(fixtures.createTuition(), fixtures.createStudent(), clazz);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, clazz.getId())
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.ASSOCIATED_RESOURCES);
            assertThat(clazzRepository.read(clazz.getId())).isPresent();
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
        void shouldNotDeleteAClazzOfAnotherOrganization() {
            final Clazz clazz = OrganizationScope.call(anotherOrganizationId, fixtures::createClazz);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, clazz.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(clazz.getId()), clazz);
        }
    }

    private static ClazzInput.CreateUpdate input(UUID teacherId) {
        return new ClazzInput.CreateUpdate(
            "Adult jiu-jitsu",
            Clazz.MartialArt.JIU_JITSU,
            LocalTime.of(19, 0),
            LocalTime.of(20, 30),
            Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            teacherId);
    }

    private ClazzOutput expectedOutput(Clazz clazz, ClazzInput.CreateUpdate input, Teacher expectedTeacher) {
        return new ClazzOutput(
            clazz == null ? null : clazz.getId(),
            input.name(),
            input.martialArt(),
            input.startTime(),
            input.endTime(),
            input.weekdays(),
            clazz == null ? null : clazz.getCreatedAt(),
            null,
            TeacherOutput.from(expectedTeacher),
            organizationId);
    }

    private Clazz readInAnotherOrganization(UUID id) {
        return OrganizationScope.call(anotherOrganizationId, () -> clazzRepository.read(id).orElseThrow());
    }
}
