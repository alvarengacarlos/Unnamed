package com.alvarengacarlos.unnamed.app;

import static com.alvarengacarlos.unnamed.app.testsupport.ApiErrors.assertFieldErrors;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import com.alvarengacarlos.unnamed.app.common.CommonOutput;
import com.alvarengacarlos.unnamed.app.rankcategory.RankCategoryOutput;
import com.alvarengacarlos.unnamed.app.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.app.testsupport.E2ETest;
import com.alvarengacarlos.unnamed.app.testsupport.Fixtures;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScoped;
import com.alvarengacarlos.unnamed.app.testsupport.ValidationProblem;

/**
 * Every list endpoint takes the same page and pageSize parameters; the rank categories stand for all of them.
 */
@E2ETest
public class PaginationApiIT implements OrganizationScoped {
    private static final String PATH = "/rank-categories";
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final ParameterizedTypeReference<CommonOutput.PaginationOutput<RankCategoryOutput>> PAGE_TYPE =
        new ParameterizedTypeReference<>() {};
    private final UUID organizationId = UUID.randomUUID();

    @Autowired
    private ApiClient api;

    @Autowired
    private Fixtures fixtures;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    @Test
    void shouldReturnTheFirstPageOf50WhenThePaginationIsMissing() {
        IntStream.range(0, DEFAULT_PAGE_SIZE + 1).forEach(_ -> fixtures.createRankCategory());

        final CommonOutput.PaginationOutput<RankCategoryOutput> page = api.client(organizationId).get()
            .uri(PATH)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(PAGE_TYPE)
            .returnResult()
            .getResponseBody();

        assertThat(page.content()).hasSize(DEFAULT_PAGE_SIZE);
        assertThat(page.page())
            .isEqualTo(new CommonOutput.PageMetadata(0, DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE + 1, 2));
    }

    @ParameterizedTest
    @CsvSource({"-1, 10, page", "1000001, 10, page", "0, 0, pageSize", "0, 51, pageSize"})
    void shouldReturnBadRequestWhenThePaginationIsOutOfRange(int pageNumber, int pageSize, String field) {
        final ValidationProblem errors = api.client(organizationId).get()
            .uri(ApiClient.paged(PATH), pageNumber, pageSize)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody(ValidationProblem.class)
            .returnResult()
            .getResponseBody();

        assertFieldErrors(errors, field);
    }
}
