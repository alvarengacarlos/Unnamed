package com.alvarengacarlos.unnamed.coreapi.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

public final class ApiErrors {
    public static final Problem ASSOCIATED_RESOURCES = Problem.of(
        "resource-in-use",
        "Resource in use",
        409,
        "There are resources associated with this resource");
    public static final Problem BULK_HAS_DUPLICATED_ITEM = Problem.of(
        "bulk-has-duplicated-item",
        "Bulk has duplicated item",
        400);
    public static final String INVALID_INPUT_TYPE = Problem.TYPE_PREFIX + "invalid-input";

    private ApiErrors() {
    }

    /**
     * Checks that the response is an invalid-input problem with one error for each field and no other. Only the field
     * is checked, because the validation messages depend on the locale of the JVM.
     */
    public static void assertFieldErrors(ValidationProblem problem, String... fields) {
        assertThat(problem.type()).isEqualTo(INVALID_INPUT_TYPE);
        assertThat(problem.status()).isEqualTo(400);
        assertThat(problem.errors())
            .extracting(ValidationProblem.Field::field)
            .containsExactlyInAnyOrder(fields);
    }
}
