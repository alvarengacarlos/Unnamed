package com.alvarengacarlos.unnamed.coreapi.rank;

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
import com.alvarengacarlos.unnamed.coreapi.rankcategory.RankCategory;
import com.alvarengacarlos.unnamed.coreapi.rankcategory.RankCategoryOutput;
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
public class RankApiIT implements OrganizationScoped {
    private static final String PATH = "/ranks";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final String NAME_FIELD = "name";
    private static final byte ORDER = 3;
    private static final String RANK_NAME = "First dan";
    private static final Problem NOT_FOUND = Problem.of("rank-not-found", "Rank not found", 404);
    private static final Problem CATEGORY_NOT_FOUND = Problem.of(
        "rank-category-not-found",
        "Rank category not found",
        404);
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<RankOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final RankInput.Update updateInput = new RankInput.Update("Second dan");
    private RankCategory rankCategory;

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private RankRepository rankRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @BeforeEach
    void beforeEach() {
        rankCategory = fixtures.createRankCategory();
    }

    @Nested
    class Create {
        @Test
        void shouldCreateARank() {
            final RankInput.Create input = new RankInput.Create(RANK_NAME, ORDER, rankCategory.getId());

            final RankOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(input)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(RankOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, new RankOutput(
                null,
                input.name(),
                input.order(),
                RankCategoryOutput.from(rankCategory),
                null,
                null,
                organizationId));
            assertSameOutput(RankOutput.from(rankRepository.read(output.id()).orElseThrow()), output);
        }

        @Test
        void shouldReturnBadRequestWhenTheInputIsInvalid() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new RankInput.Create(" ", (byte) 0, null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, NAME_FIELD, "order", "categoryId");
        }

        @Test
        void shouldReturnNotFoundWhenTheCategoryDoesNotExist() {
            api.client(organizationId).post()
                .uri(PATH)
                .body(new RankInput.Create(RANK_NAME, ORDER, UUID.randomUUID()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(CATEGORY_NOT_FOUND);
        }

        @Test
        void shouldReturnNotFoundWhenTheCategoryBelongsToAnotherOrganization() {
            final RankCategory rankCategoryOfAnotherOrganization = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createRankCategory);

            api.client(organizationId).post()
                .uri(PATH)
                .body(new RankInput.Create(RANK_NAME, ORDER, rankCategoryOfAnotherOrganization.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(CATEGORY_NOT_FOUND);
        }

        @Test
        void shouldReturnConflictWhenTheCategoryHasARankWithTheSameOrder() {
            final Rank rank = fixtures.createRank(rankCategory);

            api.client(organizationId).post()
                .uri(PATH)
                .body(new RankInput.Create(RANK_NAME, rank.getOrder(), rankCategory.getId()))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(Problem.of("rank-exists", "Rank exists", 409));
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetOnlyTheRanksOfTheOrganization() {
            final Rank rank = fixtures.createRank(rankCategory);
            OrganizationScope.run(anotherOrganizationId, fixtures::createRank);

            final CommonOutput.PaginationOutput<RankOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(RankOutput.from(rank)), 10));
        }

        @Test
        void shouldGetOnlyTheRanksOfTheCategory() {
            final Rank rank = fixtures.createRank(rankCategory);
            fixtures.createRank();

            final CommonOutput.PaginationOutput<RankOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH) + "&categoryId={categoryId}", 0, 10, rankCategory.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertSameOutput(page, Pages.onlyPage(List.of(RankOutput.from(rank)), 10));
        }

        @Test
        void shouldGetTheLastPage() {
            fixtures.createRank(rankCategory);
            fixtures.createRank(rankCategory);
            fixtures.createRank(rankCategory);

            final CommonOutput.PaginationOutput<RankOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 1, 2)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

            assertThat(page.content()).hasSize(1);
            assertThat(page.page()).isEqualTo(new CommonOutput.PageMetadata(1, 2, 3, 2));
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetARank() {
            final Rank rank = fixtures.createRank(rankCategory);

            final RankOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, rank.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(RankOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, RankOutput.from(rank));
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
            final Rank rank = OrganizationScope.call(anotherOrganizationId, fixtures::createRank);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, rank.getId())
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
        void shouldUpdateTheNameOfARank() {
            final Rank rank = fixtures.createRank(rankCategory);

            final RankOutput output = api.client(organizationId).put()
                .uri(PATH_WITH_ID, rank.getId())
                .body(updateInput)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(RankOutput.class)
                .returnResult()
                .getResponseBody();

            assertUpdatedOutput(output, RankOutput.from(rank.toBuilder().name(updateInput.name()).build()));
            assertSameOutput(RankOutput.from(rankRepository.read(rank.getId()).orElseThrow()), output);
        }

        @Test
        void shouldReturnBadRequestWhenTheNameIsBlank() {
            final Rank rank = fixtures.createRank(rankCategory);

            final ValidationProblem errors = api.client(organizationId).put()
                .uri(PATH_WITH_ID, rank.getId())
                .body(new RankInput.Update(""))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, NAME_FIELD);
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
        void shouldNotUpdateARankOfAnotherOrganization() {
            final Rank rank = OrganizationScope.call(anotherOrganizationId, fixtures::createRank);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, rank.getId())
                .body(updateInput)
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(rank.getId()), rank);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteARank() {
            final Rank rank = fixtures.createRank(rankCategory);

            final RankOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, rank.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(RankOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, RankOutput.from(rank));
            assertThat(rankRepository.read(rank.getId())).isEmpty();
        }

        @Test
        void shouldReturnConflictWhenAMemberHasIt() {
            final Rank rank = fixtures.createRank(rankCategory);
            fixtures.createMember(rank);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, rank.getId())
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.ASSOCIATED_RESOURCES);
            assertThat(rankRepository.read(rank.getId())).isPresent();
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
        void shouldNotDeleteARankOfAnotherOrganization() {
            final Rank rank = OrganizationScope.call(anotherOrganizationId, fixtures::createRank);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, rank.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(rank.getId()), rank);
        }
    }

    private Rank readInAnotherOrganization(UUID id) {
        return OrganizationScope.call(anotherOrganizationId, () -> rankRepository.read(id).orElseThrow());
    }
}
