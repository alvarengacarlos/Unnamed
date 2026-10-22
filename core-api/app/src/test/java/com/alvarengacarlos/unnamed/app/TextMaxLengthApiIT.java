package com.alvarengacarlos.unnamed.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import com.alvarengacarlos.unnamed.app.common.CommonInput;
import com.alvarengacarlos.unnamed.app.rankcategory.RankCategoryInput;
import com.alvarengacarlos.unnamed.app.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.app.testsupport.ApiErrors;
import com.alvarengacarlos.unnamed.app.testsupport.E2ETest;
import com.alvarengacarlos.unnamed.app.testsupport.OrganizationScoped;
import com.alvarengacarlos.unnamed.app.testsupport.ValidationProblem;
import com.alvarengacarlos.unnamed.app.tournament.Tournament;
import com.alvarengacarlos.unnamed.app.tournament.TournamentInput;

/**
 * Every text typed by the users, such as a name or an address, has the same limit, which the columns of the database
 * also have.
 */
@E2ETest
public class TextMaxLengthApiIT implements OrganizationScoped {
    private static final String LONGEST_TEXT = "a".repeat(CommonInput.TEXT_MAX_LENGTH);
    private static final String TOO_LONG_TEXT = LONGEST_TEXT + LONGEST_TEXT.charAt(0);
    private static final String FIELD_SEPARATOR = " ";
    private final UUID organizationId = UUID.randomUUID();

    @Autowired
    private ApiClient api;

    @Override
    public UUID organizationId() {
        return organizationId;
    }

    // The fields are separated by spaces. The other fields of the body are missing, so they are invalid as well;
    // only the text fields are checked.
    @ParameterizedTest
    @CsvSource({
        "POST, /rank-categories, name",
        "PUT, /rank-categories/00000000-0000-0000-0000-000000000000, name",
        "POST, /ranks, name",
        "PUT, /ranks/00000000-0000-0000-0000-000000000000, name",
        "POST, /members, name address",
        "PUT, /members/00000000-0000-0000-0000-000000000000, name address",
        "POST, /tuitions, name",
        "POST, /clazzes, name",
        "POST, /gradings, name where",
        "POST, /tournaments, name where",
        "POST, /tournament-matches, where",
        "PUT, /tournament-matches/00000000-0000-0000-0000-000000000000, where"
    })
    void shouldReturnBadRequestWhenATextIsTooLong(String method, String path, String fields) {
        final Map<String, String> body = Arrays.stream(fields.split(FIELD_SEPARATOR))
            .collect(Collectors.toMap(Function.identity(), _ -> TOO_LONG_TEXT));

        final ValidationProblem errors = api.client(organizationId).method(HttpMethod.valueOf(method))
            .uri(path)
            .body(body)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody(ValidationProblem.class)
            .returnResult()
            .getResponseBody();

        assertThat(errors.type()).isEqualTo(ApiErrors.INVALID_INPUT_TYPE);
        assertThat(errors.errors())
            .extracting(ValidationProblem.Field::field)
            .contains(fields.split(FIELD_SEPARATOR));
    }

    @Test
    void shouldSaveATextOfTheMaxLength() {
        api.client(organizationId).post()
            .uri("/rank-categories")
            .body(new RankCategoryInput.CreateUpdate(LONGEST_TEXT))
            .exchange()
            .expectStatus()
            .isCreated();

        api.client(organizationId).post()
            .uri("/tournaments")
            .body(new TournamentInput.CreateUpdate(
                LONGEST_TEXT,
                Tournament.TournamentFormat.SINGLE_ELIMINATION,
                new TournamentInput.DivisionCriterion(null, null, null, null, null, null),
                BigDecimal.ZERO,
                LocalDateTime.of(2026, 9, 12, 8, 0),
                LocalDateTime.of(2026, 9, 12, 18, 0),
                LONGEST_TEXT))
            .exchange()
            .expectStatus()
            .isCreated();
    }
}
