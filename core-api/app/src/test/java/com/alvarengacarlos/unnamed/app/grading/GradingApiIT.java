package com.alvarengacarlos.unnamed.app.grading;

import static com.alvarengacarlos.unnamed.app.testsupport.ApiClient.assertLocation;
import static com.alvarengacarlos.unnamed.app.testsupport.ApiErrors.assertFieldErrors;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertCreatedOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertUpdatedOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

import com.alvarengacarlos.unnamed.app.common.CommonOutput;
import com.alvarengacarlos.unnamed.app.rank.Rank;
import com.alvarengacarlos.unnamed.app.rank.RankOutput;
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
public class GradingApiIT implements OrganizationScoped {
    private static final String PATH = "/gradings";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final String PRICES_FIELD = "prices";
    private static final String BLANK = " ";
    private static final BigDecimal PRICE = new BigDecimal("80.00");
    private static final Problem NOT_FOUND = Problem.of("grading-not-found", "Grading not found", 404);
    private static final Problem RANK_NOT_FOUND = Problem.of("rank-not-found", "Rank not found", 404);
    private static final Problem REGISTRATION_PERIOD_ENDS_BEFORE_IT_STARTS = Problem.of(
        "registration-period-ends-before-it-starts",
        "Registration period ends before it starts",
        400);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<GradingOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private Rank rank;

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private GradingRepository gradingRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @BeforeEach
    void beforeEach() {
        rank = fixtures.createRank();
    }

    @Nested
    class Create {
        @Test
        void shouldCreateAGradingWithAPricePerRank() {
            final Rank anotherRank = fixtures.createRank();
            final GradingInput.CreateUpdate input = input(Map.of(
                rank.getId(), PRICE,
                anotherRank.getId(), BigDecimal.ZERO));

            final GradingOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(input)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(GradingOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, expectedOutput(null, input, Set.of(
                new GradingPriceOutput(PRICE, RankOutput.from(rank)),
                new GradingPriceOutput(BigDecimal.ZERO, RankOutput.from(anotherRank)))));
            assertSameOutput(GradingOutput.from(gradingRepository.read(output.id()).orElseThrow()), output);
        }

        @Test
        void shouldReturnBadRequestWhenTheInputIsInvalid() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new GradingInput.CreateUpdate(BLANK, null, BLANK, null, null, Map.of()))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(
                errors,
                "name",
                "when",
                "where",
                "registrationPeriodStart",
                "registrationPeriodEnd",
                PRICES_FIELD);
        }

        @Test
        void shouldReturnBadRequestWhenAPriceIsNegative() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(input(Map.of(rank.getId(), new BigDecimal("-1"))))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertThat(errors.errors()).singleElement().extracting(ValidationProblem.Field::field).asString()
                .startsWith(PRICES_FIELD);
        }

        @Test
        void shouldReturnNotFoundWhenARankBelongsToAnotherOrganization() {
            final Rank rankOfAnotherOrganization = OrganizationScope.call(anotherOrganizationId, fixtures::createRank);

            api.client(organizationId).post()
                .uri(PATH)
                .body(input(Map.of(rankOfAnotherOrganization.getId(), PRICE)))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(RANK_NOT_FOUND);
        }

        @Test
        void shouldReturnBadRequestWhenTheRegistrationPeriodEndsBeforeItStarts() {
            api.client(organizationId).post()
                .uri(PATH)
                .body(inputWithRegistrationPeriod(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 9)))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .isEqualTo(REGISTRATION_PERIOD_ENDS_BEFORE_IT_STARTS);
        }

        @Test
        void shouldCreateAGradingWhoseRegistrationPeriodIsOneDay() {
            api.client(organizationId).post()
                .uri(PATH)
                .body(inputWithRegistrationPeriod(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 10)))
                .exchange()
                .expectStatus()
                .isCreated();
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheGradingsOfTheOrganization() {
            final Grading grading = fixtures.createGrading(rank);
            OrganizationScope.run(anotherOrganizationId, fixtures::createGrading);

            final CommonOutput.PaginationOutput<GradingOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(GradingOutput.from(grading)), 10));
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetAGrading() {
            final Grading grading = fixtures.createGrading(rank);

            final GradingOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, grading.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(GradingOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, GradingOutput.from(grading));
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
            final Grading grading = OrganizationScope.call(anotherOrganizationId, fixtures::createGrading);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, grading.getId())
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
        void shouldReplaceTheDataAndThePrices() {
            final Grading grading = fixtures.createGrading(rank);
            final Rank newRank = fixtures.createRank();
            final GradingInput.CreateUpdate input = input(Map.of(newRank.getId(), PRICE));

            final GradingOutput output = api.client(organizationId).put()
                .uri(PATH_WITH_ID, grading.getId())
                .body(input)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(GradingOutput.class)
                .returnResult()
                .getResponseBody();

            assertUpdatedOutput(output, expectedOutput(
                grading,
                input,
                Set.of(new GradingPriceOutput(PRICE, RankOutput.from(newRank)))));
            assertSameOutput(GradingOutput.from(gradingRepository.read(grading.getId()).orElseThrow()), output);
        }

        @Test
        void shouldReturnNotFoundWhenItDoesNotExist() {
            api.client(organizationId).put()
                .uri(PATH_WITH_ID, UUID.randomUUID())
                .body(input(Map.of(rank.getId(), PRICE)))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldReturnBadRequestWhenTheRegistrationPeriodEndsBeforeItStarts() {
            final Grading grading = fixtures.createGrading(rank);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, grading.getId())
                .body(inputWithRegistrationPeriod(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 9)))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .isEqualTo(REGISTRATION_PERIOD_ENDS_BEFORE_IT_STARTS);
            assertSameEntity(gradingRepository.read(grading.getId()).orElseThrow(), grading);
        }

        @Test
        void shouldNotUpdateAGradingOfAnotherOrganization() {
            final Grading grading = OrganizationScope.call(anotherOrganizationId, fixtures::createGrading);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, grading.getId())
                .body(input(Map.of(rank.getId(), PRICE)))
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(grading.getId()), grading);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteAGrading() {
            final Grading grading = fixtures.createGrading(rank);

            final GradingOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, grading.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(GradingOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, GradingOutput.from(grading));
            assertThat(gradingRepository.read(grading.getId())).isEmpty();
        }

        @Test
        void shouldReturnConflictWhenACandidateHasIt() {
            final Grading grading = fixtures.createGrading(rank);
            fixtures.createGradingCandidate(grading, fixtures.createMember(rank));

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, grading.getId())
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.ASSOCIATED_RESOURCES);
            assertThat(gradingRepository.read(grading.getId())).isPresent();
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
        void shouldNotDeleteAGradingOfAnotherOrganization() {
            final Grading grading = OrganizationScope.call(anotherOrganizationId, fixtures::createGrading);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, grading.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(grading.getId()), grading);
        }
    }

    private static GradingInput.CreateUpdate input(Map<UUID, BigDecimal> prices) {
        return new GradingInput.CreateUpdate(
            "Winter grading",
            LocalDateTime.of(2026, 7, 18, 9, 0),
            "Main gym",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 7, 10),
            prices);
    }

    private GradingInput.CreateUpdate inputWithRegistrationPeriod(LocalDate start, LocalDate end) {
        final GradingInput.CreateUpdate input = input(Map.of(rank.getId(), PRICE));
        return new GradingInput.CreateUpdate(input.name(), input.when(), input.where(), start, end, input.prices());
    }

    private GradingOutput expectedOutput(
        Grading grading,
        GradingInput.CreateUpdate input,
        Set<GradingPriceOutput> gradingPrices) {
        return new GradingOutput(
            grading == null ? null : grading.getId(),
            input.name(),
            input.when(),
            input.where(),
            input.registrationPeriodStart(),
            input.registrationPeriodEnd(),
            gradingPrices,
            grading == null ? null : grading.getCreatedAt(),
            null,
            organizationId);
    }

    private Grading readInAnotherOrganization(UUID id) {
        return OrganizationScope.call(anotherOrganizationId, () -> gradingRepository.read(id).orElseThrow());
    }
}
