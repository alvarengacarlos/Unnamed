package com.alvarengacarlos.unnamed.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.sql.SQLException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Nested
    class HandleDataIntegrityViolation {
        @Test
        void shouldReturnResourceInUseWhenAForeignKeyIsViolated() {
            final ResponseEntity<ProblemDetail> response = globalExceptionHandler.handle(
                new DataIntegrityViolationException("fk", new SQLException(null, "23503")));

            assertProblem(response, HttpStatus.CONFLICT, "urn:unnamed:problem:resource-in-use", "Resource in use");
        }

        @Test
        void shouldReturnResourceExistsWhenAUniqueConstraintIsViolated() {
            final ResponseEntity<ProblemDetail> response = globalExceptionHandler.handle(
                new DuplicateKeyException("unique", new SQLException(null, "23505")));

            assertProblem(response, HttpStatus.CONFLICT, "urn:unnamed:problem:resource-exists", "Resource exists");
        }

        @Test
        void shouldReturnInternalServerErrorWithoutDetailsForOtherViolations() {
            final ResponseEntity<ProblemDetail> response = globalExceptionHandler.handle(
                new DataIntegrityViolationException("not null", new SQLException(null, "23502")));

            assertProblem(response, HttpStatus.INTERNAL_SERVER_ERROR, "about:blank", "Internal Server Error");
            assertThat(response.getBody().getDetail()).isNull();
        }

        private void assertProblem(
            ResponseEntity<ProblemDetail> response,
            HttpStatus status,
            String type,
            String title) {
            assertThat(response.getStatusCode()).isEqualTo(status);
            assertThat(response.getBody().getStatus()).isEqualTo(status.value());
            assertThat(response.getBody().getType()).isEqualTo(URI.create(type));
            assertThat(response.getBody().getTitle()).isEqualTo(title);
        }
    }
}
