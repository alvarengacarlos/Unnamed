package com.alvarengacarlos.unnamed.app.tuition;

import static com.alvarengacarlos.unnamed.app.testsupport.ApiClient.assertLocation;
import static com.alvarengacarlos.unnamed.app.testsupport.ApiErrors.assertFieldErrors;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertCreatedOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertUpdatedOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

import com.alvarengacarlos.unnamed.app.common.CommonOutput;
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
public class TuitionApiIT implements OrganizationScoped {
    private static final String PATH = "/tuitions";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final Problem NOT_FOUND = Problem.of("tuition-not-found", "Tuition not found", 404);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<TuitionOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final TuitionInput.CreateUpdate input = new TuitionInput.CreateUpdate(
        "Monthly plan",
        new BigDecimal("150.00"));

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TuitionRepository tuitionRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Nested
    class Create {
        @Test
        void shouldCreateATuition() {
            final TuitionOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(input)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(TuitionOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, new TuitionOutput(
                null,
                input.name(),
                input.price(),
                null,
                null,
                organizationId));
            assertSameOutput(TuitionOutput.from(tuitionRepository.read(output.id()).orElseThrow()), output);
        }

        @Test
        void shouldCreateAFreeTuition() {
            api.client(organizationId).post()
                .uri(PATH)
                .body(new TuitionInput.CreateUpdate("Scholarship", BigDecimal.ZERO))
                .exchange()
                .expectStatus()
                .isCreated();
        }

        @Test
        void shouldReturnBadRequestWhenTheInputIsInvalid() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new TuitionInput.CreateUpdate(" ", new BigDecimal("-0.01")))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, "name", "price");
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheTuitionsOfTheOrganization() {
            final Tuition tuition = fixtures.createTuition();
            OrganizationScope.run(anotherOrganizationId, fixtures::createTuition);

            final CommonOutput.PaginationOutput<TuitionOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(TuitionOutput.from(tuition)), 10));
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetATuition() {
            final Tuition tuition = fixtures.createTuition();

            final TuitionOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, tuition.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TuitionOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, TuitionOutput.from(tuition));
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
            final Tuition tuition = OrganizationScope.call(anotherOrganizationId, fixtures::createTuition);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, tuition.getId())
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
        void shouldUpdateATuition() {
            final Tuition tuition = fixtures.createTuition();

            final TuitionOutput output = api.client(organizationId).put()
                .uri(PATH_WITH_ID, tuition.getId())
                .body(input)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TuitionOutput.class)
                .returnResult()
                .getResponseBody();

            assertUpdatedOutput(output, TuitionOutput.from(tuition.toBuilder()
                .name(input.name())
                .price(input.price())
                .build()));
            assertSameOutput(TuitionOutput.from(tuitionRepository.read(tuition.getId()).orElseThrow()), output);
        }

        @Test
        void shouldReturnNotFoundWhenItDoesNotExist() {
            api.client(organizationId).put()
                .uri(PATH_WITH_ID, UUID.randomUUID())
                .body(input)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldNotUpdateATuitionOfAnotherOrganization() {
            final Tuition tuition = OrganizationScope.call(anotherOrganizationId, fixtures::createTuition);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, tuition.getId())
                .body(input)
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(tuition.getId()), tuition);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteATuition() {
            final Tuition tuition = fixtures.createTuition();

            final TuitionOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tuition.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TuitionOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, TuitionOutput.from(tuition));
            assertThat(tuitionRepository.read(tuition.getId())).isEmpty();
        }

        @Test
        void shouldReturnConflictWhenAnEnrollmentHasIt() {
            final Tuition tuition = fixtures.createTuition();
            fixtures.createStudentEnrollment(tuition, fixtures.createStudent(), fixtures.createClazz());

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tuition.getId())
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.ASSOCIATED_RESOURCES);
            assertThat(tuitionRepository.read(tuition.getId())).isPresent();
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
        void shouldNotDeleteATuitionOfAnotherOrganization() {
            final Tuition tuition = OrganizationScope.call(anotherOrganizationId, fixtures::createTuition);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tuition.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(tuition.getId()), tuition);
        }
    }

    private Tuition readInAnotherOrganization(UUID id) {
        return OrganizationScope.call(anotherOrganizationId, () -> tuitionRepository.read(id).orElseThrow());
    }
}
