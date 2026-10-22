package com.alvarengacarlos.unnamed.app;

import static com.alvarengacarlos.unnamed.app.testsupport.ApiErrors.assertFieldErrors;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.alvarengacarlos.unnamed.app.testsupport.ApiClient;
import com.alvarengacarlos.unnamed.app.testsupport.E2ETest;
import com.alvarengacarlos.unnamed.app.testsupport.Problem;
import com.alvarengacarlos.unnamed.app.testsupport.ValidationProblem;

/**
 * Every error is a problem detail (RFC 9457) sent as application/problem+json. Errors that only the HTTP status
 * describes have the type about:blank.
 */
@E2ETest
@ExtendWith(OutputCaptureExtension.class)
public class ErrorResponseApiIT {
    private static final String PATH = "/members";
    private static final String PATH_WITH_ID = PATH + "/{id}";
    private static final String ABOUT_BLANK = "about:blank";
    private final UUID organizationId = UUID.randomUUID();

    @Autowired
    private ApiClient api;

    @Test
    void shouldReturnNotFoundWhenTheRouteDoesNotExist() {
        final Problem problem = api.client(organizationId).get()
            .uri("/route-that-does-not-exist")
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectHeader()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(Problem.class)
            .returnResult()
            .getResponseBody();

        assertThat(problem.type()).isEqualTo(ABOUT_BLANK);
        assertThat(problem.title()).isEqualTo("Not Found");
        assertThat(problem.status()).isEqualTo(404);
    }

    @Test
    void shouldReturnMethodNotAllowedWithTheAllowedMethods() {
        final Problem problem = api.client(organizationId).patch()
            .uri(PATH)
            .exchange()
            .expectStatus()
            .isEqualTo(405)
            .expectHeader()
            .exists(HttpHeaders.ALLOW)
            .expectBody(Problem.class)
            .returnResult()
            .getResponseBody();

        assertThat(problem.type()).isEqualTo(ABOUT_BLANK);
        assertThat(problem.title()).isEqualTo("Method Not Allowed");
    }

    @Test
    void shouldReturnUnsupportedMediaTypeWhenTheContentTypeIsNotSupported() {
        final Problem problem = api.client(organizationId).post()
            .uri(PATH)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
            .body("member")
            .exchange()
            .expectStatus()
            .isEqualTo(415)
            .expectBody(Problem.class)
            .returnResult()
            .getResponseBody();

        assertThat(problem.type()).isEqualTo(ABOUT_BLANK);
        assertThat(problem.title()).isEqualTo("Unsupported Media Type");
    }

    @Test
    void shouldReturnInvalidInputWithoutFieldsWhenTheBodyIsNotJson() {
        final ValidationProblem problem = api.client(organizationId).post()
            .uri(PATH)
            .body("{")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody(ValidationProblem.class)
            .returnResult()
            .getResponseBody();

        assertFieldErrors(problem);
        assertThat(problem.detail()).isEqualTo("The request body is not valid JSON");
    }

    @Test
    void shouldReturnInvalidInputWhenAFieldHasTheWrongType() {
        final ValidationProblem problem = api.client(organizationId).post()
            .uri(PATH)
            .body("{\"weight\": \"heavy\"}")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody(ValidationProblem.class)
            .returnResult()
            .getResponseBody();

        assertFieldErrors(problem, "weight");
        assertThat(problem.errors()).singleElement().extracting(ValidationProblem.Field::message).isEqualTo(
            "is invalid");
    }

    @Test
    void shouldReturnInvalidInputWhenAQueryParameterHasTheWrongType() {
        final ValidationProblem problem = api.client(organizationId).get()
            .uri(ApiClient.paged(PATH), "abc", 1)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody(ValidationProblem.class)
            .returnResult()
            .getResponseBody();

        assertFieldErrors(problem, "page");
    }

    @Test
    void shouldLogAnErrorOfTheClientAsAWarning(CapturedOutput output) {
        api.client(organizationId).get()
            .uri(PATH_WITH_ID, UUID.randomUUID())
            .exchange()
            .expectStatus()
            .isNotFound();

        assertThat(output.getOut().lines().filter(line -> line.contains("problem:member-not-found (404)")))
            .singleElement()
            .asString()
            .contains(" WARN ");
    }

    @Test
    void shouldLogTheInvalidFieldsButNotTheirValues(CapturedOutput output) {
        final String cpf = "111.111.111-11";

        api.client(organizationId).post()
            .uri(PATH)
            .body(String.format("{\"cpf\": \"%s\", \"phone\": \"123\"}", cpf))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertThat(output.getOut())
            .containsPattern(" WARN .*Invalid input: .*cpf")
            .doesNotContain(cpf);
    }

    @Test
    void shouldIdentifyTheRequestInTheProblem() {
        api.client(organizationId).get()
            .uri(PATH_WITH_ID, UUID.randomUUID())
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("$.instance").value(String.class, instance -> assertThat(instance).startsWith(PATH + "/"));
    }
}
