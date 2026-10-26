package com.alvarengacarlos.unnamed.coreapi;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.exc.MismatchedInputException;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * Answers every error with a problem detail (RFC 9457, application/problem+json). Errors of the application have a
 * type of their own (urn:unnamed:problem:*); errors that only the HTTP status describes have the type about:blank.
 * Errors of the client (4xx) are logged as WARN and errors of the server (5xx) as ERROR. The logs name the fields and
 * constraints involved, never the values sent, which may be personal data (e.g. a CPF).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    public static final URI INVALID_INPUT_TYPE = CommonException.type("invalid-input");
    public static final URI RESOURCE_IN_USE_TYPE = CommonException.type("resource-in-use");
    public static final URI RESOURCE_EXISTS_TYPE = CommonException.type("resource-exists");
    public static final URI ABOUT_BLANK_TYPE = URI.create("about:blank");
    public static final String ERRORS_PROPERTY = "errors";
    private final String isInvalidMessage = "is invalid";
    private final String wrongTypeLog = "Invalid input: {} has the wrong type";
    private final String commonExceptionLog = "{} ({}): {}";
    private final String invalidInputTitle = "Invalid input";
    private final String invalidFieldsDetail = "The request has invalid fields";
    private final String resourceExistsMessage = "Resource exists";
    private final String foreignKeyViolationSqlState = "23503";
    private final String uniqueViolationSqlState = "23505";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handle(MethodArgumentNotValidException exception) {
        final List<CommonOutput.InvalidField> errors = exception.getFieldErrors()
            .stream()
            .map(fieldError -> new CommonOutput.InvalidField(
                fieldError.getField(),
                fieldError.isBindingFailure() ? isInvalidMessage : fieldError.getDefaultMessage()))
            .toList();
        logger.warn(
            "Invalid input: {}",
            errors.stream().map(CommonOutput.InvalidField::field).distinct().collect(Collectors.joining(", ")));
        return invalidInput(invalidFieldsDetail, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handle(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof MismatchedInputException mismatchedInputException) {
            final String field = mismatchedInputException.getPath()
                .stream()
                .map(reference -> reference.getPropertyName())
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
            if (!field.isEmpty()) {
                logger.warn(wrongTypeLog, field);
                return invalidInput(
                    invalidFieldsDetail,
                    List.of(new CommonOutput.InvalidField(field, isInvalidMessage)));
            }
        }

        logger.warn("Invalid input: the request body is not valid JSON");
        return invalidInput("The request body is not valid JSON", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handle(MethodArgumentTypeMismatchException exception) {
        logger.warn(wrongTypeLog, exception.getName());

        return invalidInput(
            invalidFieldsDetail,
            List.of(new CommonOutput.InvalidField(exception.getName(), isInvalidMessage)));
    }

    private ResponseEntity<ProblemDetail> invalidInput(String detail, List<CommonOutput.InvalidField> errors) {
        final ProblemDetail problemDetail = problemDetail(
            HttpStatus.BAD_REQUEST,
            INVALID_INPUT_TYPE,
            invalidInputTitle,
            detail);
        problemDetail.setProperty(ERRORS_PROPERTY, errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ProblemDetail> handle(CommonException exception) {
        if (exception.httpStatus.is5xxServerError()) {
            logger.error(commonExceptionLog, exception.type, exception.httpStatus.value(), exception.getMessage());
        } else {
            logger.warn(commonExceptionLog, exception.type, exception.httpStatus.value(), exception.getMessage());
        }

        return ResponseEntity.status(exception.httpStatus)
            .body(problemDetail(exception.httpStatus, exception.type, exception.title, exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handle(DataIntegrityViolationException exception) {
        if (hasSqlState(exception, foreignKeyViolationSqlState)) {
            logger.warn("Resource in use: other rows still reference it ({})", constraint(exception));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail(
                HttpStatus.CONFLICT,
                RESOURCE_IN_USE_TYPE,
                "Resource in use",
                "There are resources associated with this resource"));
        }

        // The services check for duplicates first; this covers concurrent requests that pass that check together.
        if (hasSqlState(exception, uniqueViolationSqlState)) {
            logger.warn("Resource exists: unique constraint violated ({})", constraint(exception));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail(
                HttpStatus.CONFLICT,
                RESOURCE_EXISTS_TYPE,
                resourceExistsMessage,
                resourceExistsMessage));
        }

        return handle((Exception) exception);
    }

    // The name of the violated constraint, without the message of Postgres, which shows the values (e.g. a CPF).
    private static String constraint(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof PSQLException psqlException && psqlException.getServerErrorMessage() != null) {
                return psqlException.getServerErrorMessage().getConstraint();
            }
        }
        return "unknown constraint";
    }

    private Boolean hasSqlState(Throwable exception, String sqlState) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && sqlState.equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handle(Exception exception) {
        // Errors of Spring MVC (404 of a route, 405, 415...) already carry a problem detail with type about:blank.
        if (exception instanceof ErrorResponse errorResponse) {
            if (errorResponse.getStatusCode().is5xxServerError()) {
                logger.error("Request failed: {}", exception.getMessage(), exception);
            } else {
                logger.warn("Request rejected: {}", exception.getMessage());
            }
            return ResponseEntity.status(errorResponse.getStatusCode())
                .headers(errorResponse.getHeaders())
                .body(withAboutBlankType(errorResponse.getBody()));
        }

        logger.error("Unexpected exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(withAboutBlankType(ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    // A missing type means about:blank (RFC 9457), but writing it saves the clients from knowing that.
    private static ProblemDetail withAboutBlankType(ProblemDetail problemDetail) {
        if (problemDetail.getType() == null) {
            problemDetail.setType(ABOUT_BLANK_TYPE);
        }
        return problemDetail;
    }

    private static ProblemDetail problemDetail(HttpStatus status, URI type, String title, String detail) {
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(type);
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
