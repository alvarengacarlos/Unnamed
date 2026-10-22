package com.alvarengacarlos.unnamed.app.rankcategory;

import static com.alvarengacarlos.unnamed.app.testsupport.ApiClient.assertLocation;
import static com.alvarengacarlos.unnamed.app.testsupport.ApiErrors.assertFieldErrors;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertCreatedOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameEntity;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertSameOutput;
import static com.alvarengacarlos.unnamed.app.testsupport.DataAssertions.assertUpdatedOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

import com.alvarengacarlos.unnamed.app.common.CommonOutput;
import com.alvarengacarlos.unnamed.app.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.app.testsupport.ApiErrors;
import com.alvarengacarlos.unnamed.app.testsupport.E2ETest;
import com.alvarengacarlos.unnamed.app.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScope;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScoped;
import com.alvarengacarlos.unnamed.app.testsupport.Problem;
import com.alvarengacarlos.unnamed.app.testsupport.ValidationProblem;

@E2ETest
public class RankCategoryApiIT implements OrganizationScoped {
    private static final String PATH = "/rank-categories";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final String NAME_FIELD = "name";
    private static final Problem NOT_FOUND = Problem.of("rank-category-not-found", "Rank category not found", 404);
    private final UUID organizationId = UUID.randomUUID();
    private final UUID anotherOrganizationId = UUID.randomUUID();
    private final RankCategoryInput.CreateUpdate input = new RankCategoryInput.CreateUpdate("Black belt");

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private RankCategoryRepository rankCategoryRepository;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Nested
    class Create {
        @Test
        void shouldCreateARankCategory() {
            final RankCategoryOutput output = api.client(organizationId).post()
                .uri(PATH)
                .body(input)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .value(HttpHeaders.LOCATION, location -> assertLocation(location, PATH))
                .expectBody(RankCategoryOutput.class)
                .returnResult()
                .getResponseBody();

            assertCreatedOutput(output, new RankCategoryOutput(null, input.name(), null, null, organizationId));
            assertSameOutput(RankCategoryOutput.from(rankCategoryRepository.read(output.id()).orElseThrow()), output);
        }

        @Test
        void shouldReturnTheLocationOfTheCreatedRankCategory() {
            final EntityExchangeResult<RankCategoryOutput> created = api.client(organizationId).post()
                .uri(PATH)
                .body(input)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(RankCategoryOutput.class)
                .returnResult();

            final RankCategoryOutput found = api.client(organizationId).get()
                .uri(created.getResponseHeaders().getLocation())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(RankCategoryOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(found, created.getResponseBody());
        }

        @Test
        void shouldReturnBadRequestWhenTheNameIsBlank() {
            final ValidationProblem errors = api.client(organizationId).post()
                .uri(PATH)
                .body(new RankCategoryInput.CreateUpdate(" "))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ValidationProblem.class)
                .returnResult()
                .getResponseBody();

            assertFieldErrors(errors, NAME_FIELD);
        }
    }

    @Nested
    class GetMany {
        @Test
        void shouldGetAPageOfRankCategories() {
            fixtures.createRankCategory();
            fixtures.createRankCategory();
            final RankCategory rankCategory = fixtures.createRankCategory();
            OrganizationScope.run(anotherOrganizationId, fixtures::createRankCategory);

            final CommonOutput.PaginationOutput<RankCategoryOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 0, 10)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<CommonOutput.PaginationOutput<RankCategoryOutput>>() {})
                .returnResult()
                .getResponseBody();

            assertThat(page.content()).hasSize(3);
            assertThat(page.page().totalPages()).isEqualTo(1);
            assertThat(page.content())
                .filteredOn(output -> output.id().equals(rankCategory.getId()))
                .singleElement()
                .satisfies(output -> assertSameOutput(output, RankCategoryOutput.from(rankCategory)));
        }

        @Test
        void shouldGetTheLastPage() {
            fixtures.createRankCategory();
            fixtures.createRankCategory();
            fixtures.createRankCategory();

            final CommonOutput.PaginationOutput<RankCategoryOutput> page = api.client(organizationId).get()
                .uri(ApiClient.paged(PATH), 1, 2)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<CommonOutput.PaginationOutput<RankCategoryOutput>>() {})
                .returnResult()
                .getResponseBody();

            assertThat(page.content()).hasSize(1);
            assertThat(page.page()).isEqualTo(new CommonOutput.PageMetadata(1, 2, 3, 2));
        }
    }

    @Nested
    class GetOne {
        @Test
        void shouldGetARankCategory() {
            final RankCategory rankCategory = fixtures.createRankCategory();

            final RankCategoryOutput output = api.client(organizationId).get()
                .uri(PATH_WITH_ID, rankCategory.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(RankCategoryOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, RankCategoryOutput.from(rankCategory));
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
            final RankCategory rankCategory = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createRankCategory);

            api.client(organizationId).get()
                .uri(PATH_WITH_ID, rankCategory.getId())
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
        void shouldUpdateARankCategory() {
            final RankCategory rankCategory = fixtures.createRankCategory();

            final RankCategoryOutput output = api.client(organizationId).put()
                .uri(PATH_WITH_ID, rankCategory.getId())
                .body(input)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(RankCategoryOutput.class)
                .returnResult()
                .getResponseBody();

            assertUpdatedOutput(output, RankCategoryOutput.from(rankCategory.toBuilder().name(input.name()).build()));
            assertSameOutput(RankCategoryOutput.from(rankCategoryRepository.read(output.id()).orElseThrow()), output);
        }

        @Test
        void shouldReturnBadRequestWhenTheNameIsBlank() {
            final RankCategory rankCategory = fixtures.createRankCategory();

            final ValidationProblem errors = api.client(organizationId).put()
                .uri(PATH_WITH_ID, rankCategory.getId())
                .body(new RankCategoryInput.CreateUpdate(""))
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
                .body(input)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class)
                .isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldNotUpdateARankCategoryOfAnotherOrganization() {
            final RankCategory rankCategory = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createRankCategory);

            api.client(organizationId).put()
                .uri(PATH_WITH_ID, rankCategory.getId())
                .body(input)
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(rankCategory.getId()), rankCategory);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteARankCategory() {
            final RankCategory rankCategory = fixtures.createRankCategory();

            final RankCategoryOutput output = api.client(organizationId).delete()
                .uri(PATH_WITH_ID, rankCategory.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(RankCategoryOutput.class)
                .returnResult()
                .getResponseBody();

            assertSameOutput(output, RankCategoryOutput.from(rankCategory));
            assertThat(rankCategoryRepository.read(rankCategory.getId())).isEmpty();
        }

        @Test
        void shouldReturnConflictWhenARankUsesIt() {
            final RankCategory rankCategory = fixtures.createRankCategory();
            fixtures.createRank(rankCategory);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, rankCategory.getId())
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(Problem.class)
                .isEqualTo(ApiErrors.ASSOCIATED_RESOURCES);
            assertThat(rankCategoryRepository.read(rankCategory.getId())).isPresent();
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
        void shouldNotDeleteARankCategoryOfAnotherOrganization() {
            final RankCategory rankCategory = OrganizationScope.call(
                anotherOrganizationId,
                fixtures::createRankCategory);

            api.client(organizationId).delete()
                .uri(PATH_WITH_ID, rankCategory.getId())
                .exchange()
                .expectStatus()
                .isNotFound();

            assertSameEntity(readInAnotherOrganization(rankCategory.getId()), rankCategory);
        }
    }

    private RankCategory readInAnotherOrganization(UUID id) {
        return OrganizationScope.call(anotherOrganizationId, () -> rankCategoryRepository.read(id).orElseThrow());
    }
}
