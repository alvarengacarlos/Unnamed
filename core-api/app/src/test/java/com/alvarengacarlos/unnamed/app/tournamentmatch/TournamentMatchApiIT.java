package com.alvarengacarlos.unnamed.app.tournamentmatch;

import static com.alvarengacarlos.unnamed.app.testsupport.ApiClient.assertLocation;
import static com.alvarengacarlos.unnamed.app.testsupport.ApiErrors.assertFieldErrors;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertCreatedOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertUpdatedOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

import com.alvarengacarlos.unnamed.app.common.CommonInput;
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
import com.alvarengacarlos.unnamed.app.tournament.Tournament;
import com.alvarengacarlos.unnamed.app.tournament.TournamentOutput;
import com.alvarengacarlos.unnamed.app.tournamentcompetitor.TournamentCompetitor;
import com.alvarengacarlos.unnamed.app.tournamentcompetitor.TournamentCompetitorOutput;

@E2ETest
public class TournamentMatchApiIT implements OrganizationScoped {
    private static final String PATH = "/tournament-matches";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final String BULK_PATH = PATH + "/bulk";
    private static final String WHERE = "Mat 1";
    private static final String ANOTHER_WHERE = "Mat 2";
    private static final String BLANK = " ";
    private static final String WHERE_FIELD = "where";
    // A race does not happen on every try, so the concurrent test repeats it.
    private static final int CONCURRENT_ROUNDS = 10;
    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 9, 12, 10, 30);
    private static final Problem NOT_FOUND = Problem.of(
        "tournament-match-not-found",
        "Tournament match not found",
        404);
    private static final Problem EXISTS = Problem.of("tournament-match-exists", "Tournament match exists", 409);
    private static final Problem COMPETITORS_NEED_TO_BELONG_TO_THE_TOURNAMENT =
        Problem.of(
            "tournament-competitors-need-to-belong-to-the-tournament",
            "Tournament competitors need to belong to the tournament",
            400);
    private static final Problem COMPETITOR_NOT_FOUND = Problem.of(
        "tournament-competitor-not-found",
        "Tournament competitor not found",
        404);
    private static final Problem COMPETITORS_NEED_TO_BE_DIFFERENT = Problem.of(
        "tournament-competitors-need-to-be-different",
        "Tournament competitors need to be different",
        400);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<TournamentMatchOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<TournamentMatchOutput>> LIST_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final TournamentMatchInput.Update updateInput = new TournamentMatchInput.Update(
        WHEN.plusHours(1),
        ANOTHER_WHERE,
        TournamentMatch.TournamentMatchResult.A);
    private Tournament tournament;
    private TournamentCompetitor competitorA;
    private TournamentCompetitor competitorB;

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TournamentMatchRepository tournamentMatchRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @BeforeEach
    void beforeEach() {
        tournament = fixtures.createTournament();
        competitorA = fixtures.createTournamentCompetitor(tournament);
        competitorB = fixtures.createTournamentCompetitor(tournament);
    }

    @Nested
    class Create {
        @Test
        void shouldCreateAPendingTournamentMatch() {
            final TournamentMatchOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(createInput(competitorA, competitorB))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(TournamentMatchOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, new TournamentMatchOutput(
                null,
                WHEN,
                WHERE,
                TournamentMatch.TournamentMatchResult.PENDING,
                null,
                null,
                TournamentCompetitorOutput.from(competitorA),
                TournamentCompetitorOutput.from(competitorB),
                TournamentOutput.from(tournament),
                organizationId));
            assertSameOutput(
                TournamentMatchOutput.from(tournamentMatchRepository.read(output.id()).orElseThrow()),
                output);
        }

        @Test
        void shouldReturnBadRequestWhenTheInputIsInvalid() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new TournamentMatchInput.Create(null, BLANK, null, null, null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(
                errors,
                "when",
                WHERE_FIELD,
                "tournamentCompetitorAId",
                "tournamentCompetitorBId",
                "tournamentId");
        }

        @Test
        void shouldReturnBadRequestWhenTheCompetitorsAreTheSame() {
            api.client(organizationId).post()
                .uri(PATH)
                .body(createInput(competitorA, competitorA))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .isEqualTo(COMPETITORS_NEED_TO_BE_DIFFERENT);
        }

        @Test
        void shouldReturnBadRequestWhenACompetitorIsFromAnotherTournament() {
            final TournamentCompetitor competitorOfAnotherTournament = fixtures.createTournamentCompetitor(
                fixtures.createTournament());

            api.client(organizationId).post()
                .uri(PATH)
                .body(createInput(competitorA, competitorOfAnotherTournament))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .isEqualTo(COMPETITORS_NEED_TO_BELONG_TO_THE_TOURNAMENT);
        }

        @Test
        void shouldReturnNotFoundWhenACompetitorBelongsToAnotherOrganization() {
            final TournamentCompetitor competitorOfAnotherOrganization = OrganizationScope.call(
                anotherOrganizationId,
                () -> fixtures.createTournamentCompetitor(fixtures.createTournament()));

            api.client(organizationId).post()
                .uri(PATH)
                .body(createInput(competitorA, competitorOfAnotherOrganization))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(COMPETITOR_NOT_FOUND);
        }

        @Test
        void shouldReturnConflictWhenTheMatchExists() {
            fixtures.createTournamentMatch(tournament, competitorA, competitorB);

            api.client(organizationId).post()
                .uri(PATH)
                .body(createInput(competitorA, competitorB))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(EXISTS);
        }

        @Test
        void shouldCreateOnlyOneMatchWhenTheSameMatchIsCreatedConcurrentlyWithTheCompetitorsSwapped()
            throws Exception {
            for (int round = 0; round < CONCURRENT_ROUNDS; round++) {
                final TournamentCompetitor a = fixtures.createTournamentCompetitor(tournament);
                final TournamentCompetitor b = fixtures.createTournamentCompetitor(tournament);
                final CountDownLatch start = new CountDownLatch(1);

                try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    final Future<Integer> first = executor.submit(() -> createConcurrently(start, a, b));
                    final Future<Integer> second = executor.submit(() -> createConcurrently(start, b, a));
                    start.countDown();

                    assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
                }
            }
        }

        private int createConcurrently(CountDownLatch start, TournamentCompetitor a, TournamentCompetitor b)
            throws InterruptedException {
            start.await();
            return api.client(organizationId).post()
                .uri(PATH)
                .body(createInput(a, b))
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getStatus()
                .value();
        }
    }

    @Nested
    class CreateInBulk {
        @Test
        void shouldCreateTournamentMatches() {
            final TournamentCompetitor competitorC = fixtures.createTournamentCompetitor(tournament);

            final List<TournamentMatchOutput> output = api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentMatchInput.CreateInBulk(Set.of(
                    createInput(competitorA, competitorB),
                    createInput(competitorA, competitorC))))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(LIST_TYPE)
                .returnResult()
                .getResponseBody();

            assertThat(output)
                .extracting(match -> match.competitorB().id())
                .containsExactlyInAnyOrder(competitorB.getId(), competitorC.getId());
            assertSameOutput(
                output,
                output.stream()
                    .map(match -> TournamentMatchOutput.from(
                        tournamentMatchRepository.read(match.id()).orElseThrow()))
                    .toList());
        }

        @Test
        void shouldReturnBadRequestWhenAMatchIsRepeatedWithTheCompetitorsSwapped() {
            api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentMatchInput.CreateInBulk(Set.of(
                    createInput(competitorA, competitorB),
                    createInput(competitorB, competitorA))))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.BULK_HAS_DUPLICATED_ITEM);
        }

        @Test
        void shouldReturnBadRequestWhenTheBulkIsTooLarge() {
            final Set<TournamentMatchInput.Create> bulk = IntStream.rangeClosed(0, CommonInput.BULK_MAX_SIZE)
                .mapToObj(_ -> new TournamentMatchInput.Create(
                    WHEN,
                    WHERE,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    tournament.getId()))
                .collect(Collectors.toSet());

            final ValidationProblem errors = api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentMatchInput.CreateInBulk(bulk))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, "bulk");
        }

        @Test
        void shouldReturnBadRequestWhenTheCompetitorsOfAMatchAreTheSame() {
            api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentMatchInput.CreateInBulk(Set.of(createInput(competitorA, competitorA))))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .isEqualTo(COMPETITORS_NEED_TO_BE_DIFFERENT);
        }

        @Test
        void shouldReturnBadRequestWhenAMatchIsRepeatedWithAnotherSchedule() {
            api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentMatchInput.CreateInBulk(Set.of(
                    createInput(competitorA, competitorB),
                    new TournamentMatchInput.Create(
                        WHEN.plusHours(1),
                        ANOTHER_WHERE,
                        competitorA.getId(),
                        competitorB.getId(),
                        tournament.getId()))))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.BULK_HAS_DUPLICATED_ITEM);
        }

        @Test
        void shouldReturnBadRequestWhenACompetitorIsFromAnotherTournament() {
            final TournamentCompetitor competitorOfAnotherTournament = fixtures.createTournamentCompetitor(
                fixtures.createTournament());

            api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentMatchInput.CreateInBulk(Set.of(
                    createInput(competitorA, competitorB),
                    createInput(competitorA, competitorOfAnotherTournament))))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .isEqualTo(COMPETITORS_NEED_TO_BELONG_TO_THE_TOURNAMENT);
            assertThat(tournamentMatchRepository.exists(tournament.getId(), competitorA.getId(), competitorB.getId()))
                .isFalse();
        }

        @Test
        void shouldReturnNotFoundWhenACompetitorBelongsToAnotherOrganization() {
            final TournamentCompetitor competitorOfAnotherOrganization = OrganizationScope.call(
                anotherOrganizationId,
                () -> fixtures.createTournamentCompetitor(fixtures.createTournament()));

            api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentMatchInput.CreateInBulk(Set.of(
                    createInput(competitorA, competitorOfAnotherOrganization))))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(COMPETITOR_NOT_FOUND);
        }

        @Test
        void shouldReturnNotFoundWhenTheTournamentDoesNotExist() {
            api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentMatchInput.CreateInBulk(Set.of(new TournamentMatchInput.Create(
                    WHEN,
                    WHERE,
                    competitorA.getId(),
                    competitorB.getId(),
                    UUID.randomUUID()))))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("tournament-not-found", "Tournament not found", 404));
        }

        @Test
        void shouldCreateNoneWhenAMatchExists() {
            fixtures.createTournamentMatch(tournament, competitorA, competitorB);
            final TournamentCompetitor competitorC = fixtures.createTournamentCompetitor(tournament);

            api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentMatchInput.CreateInBulk(Set.of(
                    createInput(competitorA, competitorB),
                    createInput(competitorA, competitorC))))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(EXISTS);

            assertThat(tournamentMatchRepository.exists(tournament.getId(), competitorA.getId(), competitorC.getId()))
                .isFalse();
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheTournamentMatchesOfTheOrganization() {
            final TournamentMatch tournamentMatch = fixtures.createTournamentMatch(
                tournament,
                competitorA,
                competitorB);
            OrganizationScope.run(anotherOrganizationId, fixtures::createTournamentMatch);

            final CommonOutput.PaginationOutput<TournamentMatchOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(TournamentMatchOutput.from(tournamentMatch)), 10));
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetATournamentMatch() {
            final TournamentMatch tournamentMatch = fixtures.createTournamentMatch(
                tournament,
                competitorA,
                competitorB);

            final TournamentMatchOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, tournamentMatch.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TournamentMatchOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, TournamentMatchOutput.from(tournamentMatch));
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
            final TournamentMatch tournamentMatch = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createTournamentMatch);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, tournamentMatch.getId())
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
        void shouldRescheduleAndSetTheResult() {
            final TournamentMatch tournamentMatch = fixtures.createTournamentMatch(
                tournament,
                competitorA,
                competitorB);

            final TournamentMatchOutput output = api.client(organizationId).put()
                .uri(PATH_WITH_ID, tournamentMatch.getId())
                .body(updateInput)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TournamentMatchOutput.class)
                .returnResult()
                .getResponseBody();

            assertUpdatedOutput(output, TournamentMatchOutput.from(tournamentMatch.toBuilder()
                .when(updateInput.when())
                .where(updateInput.where())
                .result(updateInput.result())
                .build()));
            assertSameOutput(
                TournamentMatchOutput.from(tournamentMatchRepository.read(tournamentMatch.getId()).orElseThrow()),
                output);
        }

        @Test
        void shouldReturnBadRequestWhenTheResultIsMissing() {
            final TournamentMatch tournamentMatch = fixtures.createTournamentMatch(
                tournament,
                competitorA,
                competitorB);

            final ValidationProblem errors = api.client(organizationId).put()
                .uri(PATH_WITH_ID, tournamentMatch.getId())
                .body(new TournamentMatchInput.Update(WHEN, WHERE, null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, "result");
        }

        @Test
        void shouldReturnBadRequestWhenTheWhereIsBlank() {
            final TournamentMatch tournamentMatch = fixtures.createTournamentMatch(
                tournament,
                competitorA,
                competitorB);

            final ValidationProblem errors = api.client(organizationId).put()
                .uri(PATH_WITH_ID, tournamentMatch.getId())
                .body(new TournamentMatchInput.Update(WHEN, BLANK, TournamentMatch.TournamentMatchResult.DRAW))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, WHERE_FIELD);
            assertSameEntity(tournamentMatchRepository.read(tournamentMatch.getId()).orElseThrow(), tournamentMatch);
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
        void shouldNotUpdateATournamentMatchOfAnotherOrganization() {
            final TournamentMatch tournamentMatch = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createTournamentMatch);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, tournamentMatch.getId())
                .body(updateInput)
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(tournamentMatch.getId()), tournamentMatch);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteATournamentMatch() {
            final TournamentMatch tournamentMatch = fixtures.createTournamentMatch(
                tournament,
                competitorA,
                competitorB);

            final TournamentMatchOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tournamentMatch.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TournamentMatchOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, TournamentMatchOutput.from(tournamentMatch));
            assertThat(tournamentMatchRepository.read(tournamentMatch.getId())).isEmpty();
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
        void shouldNotDeleteATournamentMatchOfAnotherOrganization() {
            final TournamentMatch tournamentMatch = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createTournamentMatch);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tournamentMatch.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(tournamentMatch.getId()), tournamentMatch);
        }
    }

    private TournamentMatchInput.Create createInput(TournamentCompetitor a, TournamentCompetitor b) {
        return new TournamentMatchInput.Create(WHEN, WHERE, a.getId(), b.getId(), tournament.getId());
    }

    private TournamentMatch readInAnotherOrganization(UUID id) {
        return OrganizationScope.call(anotherOrganizationId, () -> tournamentMatchRepository.read(id).orElseThrow());
    }
}
