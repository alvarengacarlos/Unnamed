package com.alvarengacarlos.unnamed.coreapi.gradingcandidate;

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

import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;
import com.alvarengacarlos.unnamed.coreapi.common.PaymentMethod;
import com.alvarengacarlos.unnamed.coreapi.grading.Grading;
import com.alvarengacarlos.unnamed.coreapi.grading.GradingOutput;
import com.alvarengacarlos.unnamed.coreapi.member.Member;
import com.alvarengacarlos.unnamed.coreapi.member.MemberOutput;
import com.alvarengacarlos.unnamed.coreapi.rank.Rank;
import com.alvarengacarlos.unnamed.coreapi.rank.RankOutput;
import com.alvarengacarlos.unnamed.coreapi.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.coreapi.testsupport.E2ETest;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.coreapi.testsupport.OrganizationScoped;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Pages;
import com.alvarengacarlos.unnamed.coreapi.testsupport.Problem;
import com.alvarengacarlos.unnamed.coreapi.testsupport.ValidationProblem;

@E2ETest
public class GradingCandidateApiIT implements OrganizationScoped {
    private static final String PATH = "/grading-candidates";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final Problem NOT_FOUND = Problem.of(
        "grading-candidate-not-found",
        "Grading candidate not found",
        404);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<GradingCandidateOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private Rank rank;
    private Grading grading;
    private Member member;

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private GradingCandidateRepository gradingCandidateRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @BeforeEach
    void beforeEach() {
        rank = fixtures.createRank();
        grading = fixtures.createGrading(rank);
        member = fixtures.createMember();
    }

    @Nested
    class Create {
        @Test
        void shouldCreateAGradingCandidate() {
            final GradingCandidateInput.Create input = createInput(grading.getId());

            final GradingCandidateOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(input)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(GradingCandidateOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, new GradingCandidateOutput(
                null,
                input.paymentMethod(),
                input.isPaid(),
                null,
                null,
                RankOutput.from(rank),
                GradingOutput.from(grading),
                MemberOutput.from(member),
                organizationId));
            assertSameOutput(
                GradingCandidateOutput.from(gradingCandidateRepository.read(output.id()).orElseThrow()),
                output);
        }

        @Test
        void shouldReturnBadRequestWhenTheInputIsInvalid() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new GradingCandidateInput.Create(null, null, null, null, null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, "paymentMethod", "isPaid", "memberId", "rankId", "gradingId");
        }

        @Test
        void shouldReturnNotFoundWhenTheGradingBelongsToAnotherOrganization() {
            final Grading gradingOfAnotherOrganization = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createGrading);

            api.client(organizationId).post()
                .uri(PATH)
                .body(createInput(gradingOfAnotherOrganization.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("grading-not-found", "Grading not found", 404));
        }

        @Test
        void shouldReturnConflictWhenTheMemberIsACandidateOfTheGrading() {
            fixtures.createGradingCandidate(grading, member);

            api.client(organizationId).post()
                .uri(PATH)
                .body(createInput(grading.getId()))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("grading-candidate-exists", "Grading candidate exists", 409));
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheGradingCandidatesOfTheOrganization() {
            final GradingCandidate gradingCandidate = fixtures.createGradingCandidate(grading, member);
            OrganizationScope.run(anotherOrganizationId, fixtures::createGradingCandidate);

            final CommonOutput.PaginationOutput<GradingCandidateOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(GradingCandidateOutput.from(gradingCandidate)), 10));
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetAGradingCandidate() {
            final GradingCandidate gradingCandidate = fixtures.createGradingCandidate(grading, member);

            final GradingCandidateOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, gradingCandidate.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(GradingCandidateOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, GradingCandidateOutput.from(gradingCandidate));
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
            final GradingCandidate gradingCandidate = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createGradingCandidate);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, gradingCandidate.getId())
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
        void shouldUpdateAGradingCandidate() {
            final GradingCandidate gradingCandidate = fixtures.createGradingCandidate(grading, member);
            final Rank newRank = fixtures.createRank();
            final GradingCandidateInput.Update input = new GradingCandidateInput.Update(
                PaymentMethod.MONEY,
                true,
                newRank.getId());

            final GradingCandidateOutput output = api.client(organizationId).put()
                .uri(PATH_WITH_ID, gradingCandidate.getId())
                .body(input)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(GradingCandidateOutput.class)
                .returnResult()
                .getResponseBody();

            assertUpdatedOutput(output, GradingCandidateOutput.from(gradingCandidate.toBuilder()
                .paymentMethod(input.paymentMethod())
                .isPaid(input.isPaid())
                .rank(newRank)
                .build()));
            assertSameOutput(
                GradingCandidateOutput.from(
                    gradingCandidateRepository.read(gradingCandidate.getId()).orElseThrow()),
                output);
        }

        @Test
        void shouldReturnNotFoundWhenItDoesNotExist() {
            api.client(organizationId).put()
                .uri(PATH_WITH_ID, UUID.randomUUID())
                .body(new GradingCandidateInput.Update(PaymentMethod.PIX, true, rank.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldNotUpdateAGradingCandidateOfAnotherOrganization() {
            final GradingCandidate gradingCandidate = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createGradingCandidate);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, gradingCandidate.getId())
                .body(new GradingCandidateInput.Update(PaymentMethod.PIX, true, rank.getId()))
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(gradingCandidate.getId()), gradingCandidate);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteAGradingCandidate() {
            final GradingCandidate gradingCandidate = fixtures.createGradingCandidate(grading, member);

            final GradingCandidateOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, gradingCandidate.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(GradingCandidateOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, GradingCandidateOutput.from(gradingCandidate));
            assertThat(gradingCandidateRepository.read(gradingCandidate.getId())).isEmpty();
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
        void shouldNotDeleteAGradingCandidateOfAnotherOrganization() {
            final GradingCandidate gradingCandidate = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createGradingCandidate);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, gradingCandidate.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(gradingCandidate.getId()), gradingCandidate);
        }
    }

    private GradingCandidateInput.Create createInput(UUID gradingId) {
        return new GradingCandidateInput.Create(PaymentMethod.PIX, false, member.getId(), rank.getId(), gradingId);
    }

    private GradingCandidate readInAnotherOrganization(UUID id) {
        return OrganizationScope.call(
            anotherOrganizationId,
            () -> gradingCandidateRepository.read(id).orElseThrow());
    }
}
