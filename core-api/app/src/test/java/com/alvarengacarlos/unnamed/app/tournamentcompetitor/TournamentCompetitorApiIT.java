package com.alvarengacarlos.unnamed.app.tournamentcompetitor;

import static com.alvarengacarlos.unnamed.app.testsupport.ApiClient.assertLocation;
import static com.alvarengacarlos.unnamed.app.testsupport.ApiErrors.assertFieldErrors;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertCreatedOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertUpdatedOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import com.alvarengacarlos.unnamed.app.common.PaymentMethod;
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
import com.alvarengacarlos.unnamed.app.tournament.Tournament;
import com.alvarengacarlos.unnamed.app.tournament.TournamentOutput;

@E2ETest
public class TournamentCompetitorApiIT implements OrganizationScoped {
    private static final String PATH = "/tournament-competitors";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final String BULK_PATH = PATH + "/bulk";
    private static final Problem NOT_FOUND = Problem.of(
        "tournament-competitor-not-found",
        "Tournament competitor not found",
        404);
    private static final Problem MEMBER_NOT_FOUND = Problem.of("member-not-found", "Member not found", 404);
    private static final Problem EXISTS = Problem.of(
        "tournament-competitor-exists",
        "Tournament competitor exists",
        409);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<TournamentCompetitorOutput>>
        PAGE_TYPE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<TournamentCompetitorOutput>> LIST_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final TournamentCompetitorInput.Update updateInput = new TournamentCompetitorInput.Update(
        PaymentMethod.MONEY,
        true);
    private Tournament tournament;
    private Member member;

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private TournamentCompetitorRepository tournamentCompetitorRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @BeforeEach
    void beforeEach() {
        tournament = fixtures.createTournament();
        member = fixtures.createMember();
    }

    @Nested
    class Create {
        @Test
        void shouldCreateATournamentCompetitorWithoutPoints() {
            final TournamentCompetitorInput.Create input = createInput(member.getId());

            final TournamentCompetitorOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(input)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(TournamentCompetitorOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, new TournamentCompetitorOutput(
                null,
                input.paymentMethod(),
                input.isPaid(),
                (short) 0,
                null,
                null,
                TournamentOutput.from(tournament),
                MemberOutput.from(member),
                organizationId));
            assertSameOutput(
                TournamentCompetitorOutput.from(tournamentCompetitorRepository.read(output.id()).orElseThrow()),
                output);
        }

        @Test
        void shouldReturnBadRequestWhenTheInputIsInvalid() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new TournamentCompetitorInput.Create(null, null, null, null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, "paymentMethod", "isPaid", "tournamentId", "memberId");
        }

        @Test
        void shouldReturnNotFoundWhenTheMemberBelongsToAnotherOrganization() {
            final Member memberOfAnotherOrganization = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createMember);

            api.client(organizationId).post()
                .uri(PATH)
                .body(createInput(memberOfAnotherOrganization.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(MEMBER_NOT_FOUND);
        }

        @Test
        void shouldReturnConflictWhenTheMemberIsACompetitorOfTheTournament() {
            fixtures.createTournamentCompetitor(tournament, member);

            api.client(organizationId).post()
                .uri(PATH)
                .body(createInput(member.getId()))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(EXISTS);
        }
    }

    @Nested
    class CreateInBulk {
        @Test
        void shouldCreateTournamentCompetitors() {
            final Member anotherMember = fixtures.createMember();

            final List<TournamentCompetitorOutput> output = api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentCompetitorInput.CreateInBulk(Set.of(
                    createInput(member.getId()),
                    createInput(anotherMember.getId()))))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(LIST_TYPE)
                .returnResult()
                .getResponseBody();

            assertThat(output)
                .extracting(competitor -> competitor.member().id())
                .containsExactlyInAnyOrder(member.getId(), anotherMember.getId());
            assertSameOutput(
                output,
                output.stream()
                    .map(competitor -> TournamentCompetitorOutput.from(
                        tournamentCompetitorRepository.read(competitor.id()).orElseThrow()))
                    .toList());
        }

        @Test
        void shouldReturnBadRequestWhenAMemberIsRepeated() {
            api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentCompetitorInput.CreateInBulk(Set.of(
                    createInput(member.getId()),
                    new TournamentCompetitorInput.Create(
                        PaymentMethod.MONEY,
                        true,
                        tournament.getId(),
                        member.getId()))))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.BULK_HAS_DUPLICATED_ITEM);
        }

        @Test
        void shouldReturnBadRequestWhenTheBulkIsTooLarge() {
            final Set<TournamentCompetitorInput.Create> bulk = IntStream.rangeClosed(0, CommonInput.BULK_MAX_SIZE)
                .mapToObj(_ -> createInput(UUID.randomUUID()))
                .collect(Collectors.toSet());

            final ValidationProblem errors = api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentCompetitorInput.CreateInBulk(bulk))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, "bulk");
        }

        @Test
        void shouldCreateNoneWhenAMemberIsACompetitorOfTheTournament() {
            fixtures.createTournamentCompetitor(tournament, member);
            final Member anotherMember = fixtures.createMember();

            api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentCompetitorInput.CreateInBulk(Set.of(
                    createInput(member.getId()),
                    createInput(anotherMember.getId()))))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(EXISTS);

            assertThat(tournamentCompetitorRepository.exists(tournament.getId(), anotherMember.getId())).isFalse();
        }

        @Test
        void shouldCreateNoneWhenAMemberBelongsToAnotherOrganization() {
            final Member memberOfAnotherOrganization = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createMember);

            api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentCompetitorInput.CreateInBulk(Set.of(
                    createInput(member.getId()),
                    createInput(memberOfAnotherOrganization.getId()))))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(MEMBER_NOT_FOUND);

            assertThat(tournamentCompetitorRepository.exists(tournament.getId(), member.getId())).isFalse();
        }

        @Test
        void shouldReturnNotFoundWhenATournamentDoesNotExist() {
            api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentCompetitorInput.CreateInBulk(Set.of(
                    createInput(member.getId()),
                    new TournamentCompetitorInput.Create(
                        PaymentMethod.PIX,
                        false,
                        UUID.randomUUID(),
                        fixtures.createMember().getId()))))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("tournament-not-found", "Tournament not found", 404));
        }

        @Test
        void shouldCreateTheSameMemberInDifferentTournaments() {
            final Tournament anotherTournament = fixtures.createTournament();

            final List<TournamentCompetitorOutput> output = api.client(organizationId).post()
                .uri(BULK_PATH)
                .body(new TournamentCompetitorInput.CreateInBulk(Set.of(
                    createInput(member.getId()),
                    new TournamentCompetitorInput.Create(
                        PaymentMethod.PIX,
                        false,
                        anotherTournament.getId(),
                        member.getId()))))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(LIST_TYPE)
                .returnResult()
                .getResponseBody();

            assertThat(output)
                .extracting(competitor -> competitor.tournament().id())
                .containsExactlyInAnyOrder(tournament.getId(), anotherTournament.getId());
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheTournamentCompetitorsOfTheOrganization() {
            final TournamentCompetitor tournamentCompetitor = fixtures.createTournamentCompetitor(tournament, member);
            OrganizationScope.run(
                anotherOrganizationId,
                () -> fixtures.createTournamentCompetitor(fixtures.createTournament()));

            final CommonOutput.PaginationOutput<TournamentCompetitorOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(TournamentCompetitorOutput.from(tournamentCompetitor)), 10));
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetATournamentCompetitor() {
            final TournamentCompetitor tournamentCompetitor = fixtures.createTournamentCompetitor(tournament, member);

            final TournamentCompetitorOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, tournamentCompetitor.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TournamentCompetitorOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, TournamentCompetitorOutput.from(tournamentCompetitor));
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
            final TournamentCompetitor tournamentCompetitor = createInAnotherOrganization();

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, tournamentCompetitor.getId())
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
        void shouldUpdateThePayment() {
            final TournamentCompetitor tournamentCompetitor = fixtures.createTournamentCompetitor(tournament, member);

            final TournamentCompetitorOutput output = api.client(organizationId).put()
                .uri(PATH_WITH_ID, tournamentCompetitor.getId())
                .body(updateInput)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TournamentCompetitorOutput.class)
                .returnResult()
                .getResponseBody();

            assertUpdatedOutput(output, TournamentCompetitorOutput.from(tournamentCompetitor.toBuilder()
                .paymentMethod(updateInput.paymentMethod())
                .isPaid(updateInput.isPaid())
                .build()));
            assertSameOutput(
                TournamentCompetitorOutput.from(
                    tournamentCompetitorRepository.read(tournamentCompetitor.getId()).orElseThrow()),
                output);
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
        void shouldNotUpdateATournamentCompetitorOfAnotherOrganization() {
            final TournamentCompetitor tournamentCompetitor = createInAnotherOrganization();

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, tournamentCompetitor.getId())
                .body(updateInput)
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(tournamentCompetitor.getId()), tournamentCompetitor);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteATournamentCompetitor() {
            final TournamentCompetitor tournamentCompetitor = fixtures.createTournamentCompetitor(tournament, member);

            final TournamentCompetitorOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tournamentCompetitor.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TournamentCompetitorOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, TournamentCompetitorOutput.from(tournamentCompetitor));
            assertThat(tournamentCompetitorRepository.read(tournamentCompetitor.getId())).isEmpty();
        }

        @Test
        void shouldReturnConflictWhenAMatchHasIt() {
            final TournamentCompetitor tournamentCompetitor = fixtures.createTournamentCompetitor(tournament, member);
            fixtures.createTournamentMatch(
                tournament,
                tournamentCompetitor,
                fixtures.createTournamentCompetitor(tournament));

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tournamentCompetitor.getId())
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.ASSOCIATED_RESOURCES);
            assertThat(tournamentCompetitorRepository.read(tournamentCompetitor.getId())).isPresent();
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
        void shouldNotDeleteATournamentCompetitorOfAnotherOrganization() {
            final TournamentCompetitor tournamentCompetitor = createInAnotherOrganization();

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, tournamentCompetitor.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(tournamentCompetitor.getId()), tournamentCompetitor);
        }
    }

    private TournamentCompetitorInput.Create createInput(UUID memberId) {
        return new TournamentCompetitorInput.Create(PaymentMethod.PIX, false, tournament.getId(), memberId);
    }

    private TournamentCompetitor createInAnotherOrganization() {
        return OrganizationScope.call(
            anotherOrganizationId,
            () -> fixtures.createTournamentCompetitor(fixtures.createTournament()));
    }

    private TournamentCompetitor readInAnotherOrganization(UUID id) {
        return OrganizationScope.call(
            anotherOrganizationId,
            () -> tournamentCompetitorRepository.read(id).orElseThrow());
    }
}
