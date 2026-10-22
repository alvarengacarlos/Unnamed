package com.alvarengacarlos.unnamed.app.tournament;

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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

import com.alvarengacarlos.unnamed.app.common.CommonOutput;
import com.alvarengacarlos.unnamed.app.member.Member;
import com.alvarengacarlos.unnamed.app.member.MemberOutput;
import com.alvarengacarlos.unnamed.app.rank.Rank;
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
public class TournamentApiIT implements OrganizationScoped {
    private static final String PATH = "/tournaments";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final String POSSIBLE_COMPETITORS_PATH = PATH_WITH_ID + "/possible-competitors";
    private static final BigDecimal WEIGHT = new BigDecimal("70.00");
    private static final BigDecimal NEGATIVE = new BigDecimal("-1");
    private static final Problem NOT_FOUND = Problem.of("tournament-not-found", "Tournament not found", 404);
    private static final Problem ENDS_BEFORE_IT_STARTS = Problem.of(
        "tournament-ends-before-it-starts",
        "Tournament ends before it starts",
        400);
    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 12, 8, 0);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<TournamentOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<MemberOutput>> MEMBERS_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private Rank rank;

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TournamentRepository tournamentRepository;

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
        void shouldCreateATournament() {
            final TournamentInput.CreateUpdate input = input(divisionCriterionInput(rank.getId()));

            final TournamentOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(input)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(TournamentOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, expectedOutput(null, input));
            final Tournament tournament = tournamentRepository.read(output.id()).orElseThrow();
            assertSameOutput(TournamentOutput.from(tournament), output);
            assertSameEntity(tournament.getTournamentDivisionCriterion(), divisionCriterion(Optional.of(rank)));
        }

        @Test
        void shouldCreateATournamentWithoutDivisionCriteria() {
            final TournamentOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(input(new TournamentInput.DivisionCriterion(null, null, null, null, null, null)))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(TournamentOutput.class)
                .returnResult()
                .getResponseBody();

            assertThat(output.divisionCriterion()).isEmpty();
        }

        @Test
        void shouldReturnBadRequestWhenTheInputIsInvalid() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new TournamentInput.CreateUpdate(" ", null, null, NEGATIVE, null, null, ""))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(
                errors,
                "name",
                "format",
                "divisionCriterion",
                "participationPrice",
                "startDateTime",
                "endDateTime",
                "where");
        }

        @Test
        void shouldReturnBadRequestWhenItEndsBeforeItStarts() {
            api.client(organizationId).post()
                .uri(PATH)
                .body(inputWithPeriod(START, START.minusMinutes(1)))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .isEqualTo(ENDS_BEFORE_IT_STARTS);
        }

        @Test
        void shouldCreateATournamentThatEndsWhenItStarts() {
            api.client(organizationId).post()
                .uri(PATH)
                .body(inputWithPeriod(START, START))
                .exchange()
                .expectStatus()
                .isCreated();
        }

        @Test
        void shouldReturnBadRequestWhenTheDivisionCriterionIsInvalid() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(input(new TournamentInput.DivisionCriterion(
                    null,
                    NEGATIVE,
                    BigDecimal.ZERO,
                    (byte) 0,
                    null,
                    null)))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(
                errors,
                "divisionCriterion.weight",
                "divisionCriterion.height",
                "divisionCriterion.age");
        }

        @Test
        void shouldReturnNotFoundWhenTheRankBelongsToAnotherOrganization() {
            final Rank rankOfAnotherOrganization = OrganizationScope.call(anotherOrganizationId, fixtures::createRank);

            api.client(organizationId).post()
                .uri(PATH)
                .body(input(divisionCriterionInput(rankOfAnotherOrganization.getId())))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("rank-not-found", "Rank not found", 404));
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheTournamentsOfTheOrganization() {
            final Tournament tournament = fixtures.createTournament();
            OrganizationScope.run(anotherOrganizationId, fixtures::createTournament);

            final CommonOutput.PaginationOutput<TournamentOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(TournamentOutput.from(tournament)), 10));
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetATournament() {
            final Tournament tournament = fixtures.createTournament();

            final TournamentOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, tournament.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TournamentOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, TournamentOutput.from(tournament));
        }

        @Test
        void shouldReturnTheDivisionCriterionKeyedByItsName() {
            final Tournament tournament = fixtures.createTournament(builder -> builder
                .tournamentDivisionCriterion(divisionCriterion(Optional.of(rank))));

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, tournament.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.divisionCriterion.GENDER").isEqualTo(Member.Gender.FEMALE.name())
                .jsonPath("$.divisionCriterion.WEIGHT").isEqualTo(WEIGHT.doubleValue())
                .jsonPath("$.divisionCriterion.HEIGHT").doesNotExist()
                .jsonPath("$.divisionCriterion.AGE").isEqualTo(25)
                .jsonPath("$.divisionCriterion.IS_PWD").isEqualTo(false)
                .jsonPath("$.divisionCriterion.RANK.id").isEqualTo(rank.getId().toString());
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
            final Tournament tournament = OrganizationScope.call(anotherOrganizationId, fixtures::createTournament);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, tournament.getId())
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
        void shouldUpdateATournament() {
            final Tournament tournament = fixtures.createTournament();
            final TournamentInput.CreateUpdate input = input(divisionCriterionInput(rank.getId()));

            final TournamentOutput output = api.client(organizationId).put()
                .uri(PATH_WITH_ID, tournament.getId())
                .body(input)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TournamentOutput.class)
                .returnResult()
                .getResponseBody();

            assertUpdatedOutput(output, expectedOutput(tournament, input));
            final Tournament updatedTournament = tournamentRepository.read(tournament.getId()).orElseThrow();
            assertSameOutput(TournamentOutput.from(updatedTournament), output);
            assertSameEntity(
                updatedTournament.getTournamentDivisionCriterion(),
                divisionCriterion(Optional.of(rank)));
        }

        @Test
        void shouldReturnNotFoundWhenItDoesNotExist() {
            api.client(organizationId).put()
                .uri(PATH_WITH_ID, UUID.randomUUID())
                .body(input(divisionCriterionInput(null)))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldReturnBadRequestWhenItEndsBeforeItStarts() {
            final Tournament tournament = fixtures.createTournament();

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, tournament.getId())
                .body(inputWithPeriod(START, START.minusMinutes(1)))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .isEqualTo(ENDS_BEFORE_IT_STARTS);
            assertSameEntity(tournamentRepository.read(tournament.getId()).orElseThrow(), tournament);
        }

        @Test
        void shouldNotUpdateATournamentOfAnotherOrganization() {
            final Tournament tournament = OrganizationScope.call(anotherOrganizationId, fixtures::createTournament);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, tournament.getId())
                .body(input(divisionCriterionInput(null)))
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(tournament.getId()), tournament);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteATournament() {
            final Tournament tournament = fixtures.createTournament();

            final TournamentOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tournament.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TournamentOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, TournamentOutput.from(tournament));
            assertThat(tournamentRepository.read(tournament.getId())).isEmpty();
        }

        @Test
        void shouldReturnConflictWhenACompetitorHasIt() {
            final Tournament tournament = fixtures.createTournament();
            fixtures.createTournamentCompetitor(tournament);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tournament.getId())
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.ASSOCIATED_RESOURCES);
            assertThat(tournamentRepository.read(tournament.getId())).isPresent();
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
        void shouldNotDeleteATournamentOfAnotherOrganization() {
            final Tournament tournament = OrganizationScope.call(anotherOrganizationId, fixtures::createTournament);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tournament.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(tournament.getId()), tournament);
        }
    }

    @Nested
    class GetPossibleCompetitors {
        @Test
        void shouldGetTheCompetitorsThatMatchTheDivisionCriterion() {
            final Tournament tournament = fixtures.createTournament(builder -> builder
                .tournamentDivisionCriterion(new TournamentDivisionCriterion.Builder()
                    .gender(Optional.of(Member.Gender.FEMALE))
                    .weight(Optional.of(WEIGHT))
                    .isPwd(Optional.of(false))
                    .build()));
            final Member competitor = createCompetitor(Member.Gender.FEMALE, new BigDecimal("74.00"), true);
            createCompetitor(Member.Gender.MALE, WEIGHT, true);
            createCompetitor(Member.Gender.FEMALE, new BigDecimal("76.00"), true);
            createCompetitor(Member.Gender.FEMALE, WEIGHT, false);
            OrganizationScope.run(
                anotherOrganizationId,
                () -> createCompetitor(Member.Gender.FEMALE, WEIGHT, true));

            final List<MemberOutput> output = api.client(organizationId).get()
                .uri(POSSIBLE_COMPETITORS_PATH + "?weightDifference={weightDifference}", tournament.getId(), 5)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(MEMBERS_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, List.of(MemberOutput.from(competitor)));
        }

        @Test
        void shouldReturnBadRequestWhenTheTournamentIsATitleMatch() {
            final Tournament tournament = fixtures.createTournament(builder -> builder
                .format(Tournament.TournamentFormat.TITLE_MATCH));

            api.client(organizationId).get()
                .uri(POSSIBLE_COMPETITORS_PATH, tournament.getId())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .isEqualTo(Problem.of(
                    "tournament-format-not-supported",
                    "Tournament format is not supported for this operation",
                    400,
                    "Tournament format TITLE_MATCH is not supported for this operation"));
        }

        @Test
        void shouldReturnNotFoundWhenTheTournamentDoesNotExist() {
            api.client(organizationId).get()
                .uri(POSSIBLE_COMPETITORS_PATH, UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldGetTheCompetitorsWithinTheHeightDifference() {
            final Tournament tournament = createTournament(new TournamentDivisionCriterion.Builder()
                .height(Optional.of(new BigDecimal("1.70")))
                .build());
            final Member competitor = fixtures.createMember(rank, builder -> builder
                .height(new BigDecimal("1.75"))
                .isCompetitor(true));
            fixtures.createMember(rank, builder -> builder
                .height(new BigDecimal("1.76"))
                .isCompetitor(true));

            assertPossibleCompetitors(
                tournament,
                "?heightDifference={heightDifference}",
                new BigDecimal("0.05"),
                competitor);
        }

        @Test
        void shouldGetTheCompetitorsWithinTheAgeDifference() {
            final Tournament tournament = createTournament(new TournamentDivisionCriterion.Builder()
                .age(Optional.of((byte) 25))
                .build());
            final LocalDate today = LocalDate.now();
            final Member competitor = fixtures.createMember(rank, builder -> builder
                .birthDate(today.minusYears(27))
                .isCompetitor(true));
            fixtures.createMember(rank, builder -> builder
                .birthDate(today.minusYears(28))
                .isCompetitor(true));
            fixtures.createMember(rank, builder -> builder
                .birthDate(today.minusYears(22))
                .isCompetitor(true));

            assertPossibleCompetitors(tournament, "?ageDifference={ageDifference}", 2, competitor);
        }

        @Test
        void shouldGetTheCompetitorsOfTheRank() {
            final Tournament tournament = createTournament(new TournamentDivisionCriterion.Builder()
                .rank(Optional.of(rank))
                .build());
            final Member competitor = fixtures.createMember(rank, builder -> builder.isCompetitor(true));
            fixtures.createMember(fixtures.createRank(), builder -> builder.isCompetitor(true));

            assertPossibleCompetitors(tournament, "", null, competitor);
        }

        @Test
        void shouldIgnoreACriterionWithoutItsDifference() {
            final Tournament tournament = createTournament(new TournamentDivisionCriterion.Builder()
                .weight(Optional.of(WEIGHT))
                .build());
            final Member lighter = createCompetitor(Member.Gender.MALE, new BigDecimal("55.00"), true);
            final Member heavier = createCompetitor(Member.Gender.FEMALE, new BigDecimal("120.00"), true);

            assertPossibleCompetitors(tournament, "", null, lighter, heavier);
        }

        private Tournament createTournament(TournamentDivisionCriterion divisionCriterion) {
            return fixtures.createTournament(builder -> builder.tournamentDivisionCriterion(divisionCriterion));
        }

        private void assertPossibleCompetitors(
            Tournament tournament,
            String query,
            Object difference,
            Member... expected) {
            final List<MemberOutput> output = api.client(organizationId).get()
                .uri(POSSIBLE_COMPETITORS_PATH + query, tournament.getId(), difference)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(MEMBERS_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, Arrays.stream(expected).map(MemberOutput::from).toList());
        }

        private Member createCompetitor(Member.Gender gender, BigDecimal weight, boolean isCompetitor) {
            return fixtures.createMember(fixtures.createRank(), builder -> builder
                .gender(gender)
                .weight(weight)
                .isPwd(false)
                .isCompetitor(isCompetitor));
        }
    }

    private static TournamentInput.DivisionCriterion divisionCriterionInput(UUID rankId) {
        return new TournamentInput.DivisionCriterion(Member.Gender.FEMALE, WEIGHT, null, (byte) 25, false, rankId);
    }

    private static TournamentDivisionCriterion divisionCriterion(Optional<Rank> expectedRank) {
        return new TournamentDivisionCriterion.Builder()
            .gender(Optional.of(Member.Gender.FEMALE))
            .weight(Optional.of(WEIGHT))
            .age(Optional.of((byte) 25))
            .isPwd(Optional.of(false))
            .rank(expectedRank)
            .build();
    }

    private static TournamentInput.CreateUpdate input(TournamentInput.DivisionCriterion divisionCriterion) {
        return new TournamentInput.CreateUpdate(
            "Spring open",
            Tournament.TournamentFormat.SINGLE_ELIMINATION,
            divisionCriterion,
            new BigDecimal("50.00"),
            START,
            LocalDateTime.of(2026, 9, 12, 18, 0),
            "Municipal gym");
    }

    private static TournamentInput.CreateUpdate inputWithPeriod(LocalDateTime start, LocalDateTime end) {
        final TournamentInput.CreateUpdate input = input(divisionCriterionInput(null));
        return new TournamentInput.CreateUpdate(
            input.name(),
            input.format(),
            input.divisionCriterion(),
            input.participationPrice(),
            start,
            end,
            input.where());
    }

    private TournamentOutput expectedOutput(Tournament tournament, TournamentInput.CreateUpdate input) {
        return new TournamentOutput(
            tournament == null ? null : tournament.getId(),
            input.name(),
            input.format(),
            null,
            input.participationPrice(),
            input.startDateTime(),
            input.endDateTime(),
            input.where(),
            tournament == null ? null : tournament.getCreatedAt(),
            null,
            organizationId);
    }

    private Tournament readInAnotherOrganization(UUID id) {
        return OrganizationScope.call(anotherOrganizationId, () -> tournamentRepository.read(id).orElseThrow());
    }
}
