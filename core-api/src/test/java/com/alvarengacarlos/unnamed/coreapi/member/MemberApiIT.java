package com.alvarengacarlos.unnamed.coreapi.member;

import static com.alvarengacarlos.unnamed.coreapi.testsupport.ApiClient.assertLocation;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.ApiErrors.assertFieldErrors;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertCreatedOutput;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameEntity;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertSameOutput;
import static com.alvarengacarlos.unnamed.coreapi.testsupport.DataAssertions.assertUpdatedOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;
import com.alvarengacarlos.unnamed.coreapi.rank.Rank;
import com.alvarengacarlos.unnamed.coreapi.rank.RankOutput;
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
public class MemberApiIT implements OrganizationScoped {
    private static final String PATH = "/members";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final String CPF_FILTER = "&cpf={cpf}";
    private static final String CPF_FIELD = "cpf";
    private static final String TOO_SHORT = "123";
    private static final Problem NOT_FOUND = Problem.of("member-not-found", "Member not found", 404);
    private static final Problem EXISTS = Problem.of("member-exists", "Member exists", 409);
    private static final Problem RANK_NOT_FOUND = Problem.of("rank-not-found", "Rank not found", 404);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<MemberOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private Rank rank;

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private MemberRepository memberRepository;

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
        void shouldCreateAMember() {
            final MemberInput.CreateUpdate input = input(rank.getId(), Fixtures.randomCpf());

            final MemberOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(input)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(MemberOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, expectedOutput(input));
            assertSameOutput(MemberOutput.from(memberRepository.read(output.id()).orElseThrow()), output);
        }

        @Test
        void shouldReturnBadRequestWhenTheInputIsInvalid() {
            final MemberInput.CreateUpdate input = new MemberInput.CreateUpdate(
                " ",
                TOO_SHORT,
                "111.111.111-11",
                "",
                null,
                new BigDecimal("-1"),
                new BigDecimal("1000.123"),
                null,
                null,
                null,
                null);

            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(input)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(
                errors,
                "name",
                "phone",
                CPF_FIELD,
                "address",
                "gender",
                "weight",
                "height",
                "birthDate",
                "isPwd",
                "isCompetitor",
                "rankId");
        }

        @Test
        void shouldReturnNotFoundWhenTheRankBelongsToAnotherOrganization() {
            final Rank rankOfAnotherOrganization = OrganizationScope.call(anotherOrganizationId, fixtures::createRank);

            api.client(organizationId).post()
                .uri(PATH)
                .body(input(rankOfAnotherOrganization.getId(), Fixtures.randomCpf()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(RANK_NOT_FOUND);
        }

        @Test
        void shouldReturnConflictWhenTheCpfIsTaken() {
            final Member member = fixtures.createMember(rank);

            api.client(organizationId).post()
                .uri(PATH)
                .body(input(rank.getId(), member.getCpf()))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(EXISTS);
        }

        @Test
        void shouldCreateAMemberWithACpfTakenInAnotherOrganization() {
            final Member member = OrganizationScope.call(anotherOrganizationId, fixtures::createMember);

            api.client(organizationId).post()
                .uri(PATH)
                .body(input(rank.getId(), member.getCpf()))
                .exchange()
                .expectStatus()
                .isCreated();
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheMembersOfTheOrganization() {
            final Member member = fixtures.createMember(rank);
            OrganizationScope.run(anotherOrganizationId, fixtures::createMember);

            final CommonOutput.PaginationOutput<MemberOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(MemberOutput.from(member)), 10));
        }

        @Test
        void shouldFilterByCpf() {
            final Member member = fixtures.createMember(rank);
            fixtures.createMember(rank);

            final CommonOutput.PaginationOutput<MemberOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH) + CPF_FILTER, 0, 10, member.getCpf())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(MemberOutput.from(member)), 10));
        }

        @Test
        void shouldReturnBadRequestWhenTheCpfFilterIsInvalid() {
            final ValidationProblem errors = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH) + CPF_FILTER, 0, 10, TOO_SHORT)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, CPF_FIELD);
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetAMember() {
            final Member member = fixtures.createMember(rank);

            final MemberOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, member.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(MemberOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, MemberOutput.from(member));
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
            final Member member = OrganizationScope.call(anotherOrganizationId, fixtures::createMember);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, member.getId())
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
        void shouldUpdateAMember() {
            final Member member = fixtures.createMember(rank);
            final Rank newRank = fixtures.createRank();
            final MemberInput.CreateUpdate input = input(newRank.getId(), Fixtures.randomCpf());

            final MemberOutput output = api.client(organizationId).put()
                .uri(PATH_WITH_ID, member.getId())
                .body(input)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(MemberOutput.class)
                .returnResult()
                .getResponseBody();

            assertUpdatedOutput(output, expectedOutput(input, newRank, member));
            assertSameOutput(MemberOutput.from(memberRepository.read(member.getId()).orElseThrow()), output);
        }

        @Test
        void shouldKeepTheCpfOfTheMember() {
            final Member member = fixtures.createMember(rank);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, member.getId())
                .body(input(rank.getId(), member.getCpf()))
                .exchange()
                .expectStatus()
                .isOk();
        }

        @Test
        void shouldReturnConflictWhenTheCpfBelongsToAnotherMember() {
            final Member member = fixtures.createMember(rank);
            final Member anotherMember = fixtures.createMember(rank);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, member.getId())
                .body(input(rank.getId(), anotherMember.getCpf()))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(EXISTS);
        }

        @Test
        void shouldReturnNotFoundWhenItDoesNotExist() {
            api.client(organizationId).put()
                .uri(PATH_WITH_ID, UUID.randomUUID())
                .body(input(rank.getId(), Fixtures.randomCpf()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldNotUpdateAMemberOfAnotherOrganization() {
            final Member member = OrganizationScope.call(anotherOrganizationId, fixtures::createMember);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, member.getId())
                .body(input(rank.getId(), Fixtures.randomCpf()))
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(member.getId()), member);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteAMember() {
            final Member member = fixtures.createMember(rank);

            final MemberOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, member.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(MemberOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, MemberOutput.from(member));
            assertThat(memberRepository.read(member.getId())).isEmpty();
        }

        @Test
        void shouldReturnConflictWhenAStudentHasIt() {
            final Member member = fixtures.createMember(rank);
            fixtures.createStudent(member);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, member.getId())
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.ASSOCIATED_RESOURCES);
            assertThat(memberRepository.read(member.getId())).isPresent();
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
        void shouldNotDeleteAMemberOfAnotherOrganization() {
            final Member member = OrganizationScope.call(anotherOrganizationId, fixtures::createMember);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, member.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(member.getId()), member);
        }
    }

    private static MemberInput.CreateUpdate input(UUID rankId, String cpf) {
        return new MemberInput.CreateUpdate(
            "Maria Silva",
            Fixtures.randomPhone(),
            cpf,
            "Rua das Flores, 100",
            Member.Gender.FEMALE,
            new BigDecimal("62.50"),
            new BigDecimal("1.68"),
            LocalDate.of(1995, 4, 12),
            false,
            true,
            rankId);
    }

    private MemberOutput expectedOutput(MemberInput.CreateUpdate input) {
        return expectedOutput(input, rank, null);
    }

    private MemberOutput expectedOutput(MemberInput.CreateUpdate input, Rank expectedRank, Member member) {
        return new MemberOutput(
            member == null ? null : member.getId(),
            input.name(),
            input.phone(),
            input.cpf(),
            input.address(),
            input.gender(),
            input.weight(),
            input.height(),
            input.birthDate(),
            input.isPwd(),
            input.isCompetitor(),
            member == null ? null : member.getCreatedAt(),
            null,
            RankOutput.from(expectedRank),
            organizationId);
    }

    private Member readInAnotherOrganization(UUID id) {
        return OrganizationScope.call(anotherOrganizationId, () -> memberRepository.read(id).orElseThrow());
    }
}
