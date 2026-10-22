package com.alvarengacarlos.unnamed.app.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;

/**
 * Compares entities and outputs field by field, nested objects included.
 */
public final class DataAssertions {
    // Postgres keeps microseconds and rounds the nanoseconds, so the values may differ by up to 1 µs.
    private static final Duration TIME_TOLERANCE = Duration.of(1, ChronoUnit.MICROS);
    private static final Comparator<LocalDateTime> LOCAL_DATE_TIME_COMPARATOR = (actual, expected) ->
        Duration.between(actual, expected).abs().compareTo(TIME_TOLERANCE) <= 0 ? 0 : actual.compareTo(expected);
    private static final Comparator<Instant> INSTANT_COMPARATOR = (actual, expected) ->
        Duration.between(actual, expected).abs().compareTo(TIME_TOLERANCE) <= 0 ? 0 : actual.compareTo(expected);
    private static final Comparator<LocalTime> LOCAL_TIME_COMPARATOR = (actual, expected) ->
        Duration.between(actual, expected).abs().compareTo(TIME_TOLERANCE) <= 0 ? 0 : actual.compareTo(expected);
    // A deserialized TournamentOutput holds the division criterion values untyped (e.g. "MALE" instead of
    // Member.Gender.MALE), so the tournament tests check it on the JSON instead.
    private static final String DIVISION_CRITERION_OUTPUT_FIELD = "(.*\\.)?divisionCriterion";
    private static final String UPDATED_AT_FIELD = "updatedAt";

    private DataAssertions() {
    }

    /**
     * For the output of a resource the request created: its id and timestamps come from the server.
     */
    public static void assertCreatedOutput(Object actual, Object expected) {
        assertSameOutput(actual, expected, "id", "createdAt", UPDATED_AT_FIELD);
    }

    /**
     * For the output of a resource the request updated: its updatedAt comes from the server.
     */
    public static void assertUpdatedOutput(Object actual, Object expected) {
        assertSameOutput(actual, expected, UPDATED_AT_FIELD);
    }

    public static void assertSameEntity(Object actual, Object expected, String... ignoredFields) {
        assertThat(actual)
            .usingRecursiveComparison(configuration(ignoredFields))
            .isEqualTo(expected);
    }

    public static void assertSameOutput(Object actual, Object expected, String... ignoredFields) {
        final RecursiveComparisonConfiguration configuration = configuration(ignoredFields);
        configuration.ignoreFieldsMatchingRegexes(DIVISION_CRITERION_OUTPUT_FIELD);
        assertThat(actual)
            .usingRecursiveComparison(configuration)
            .isEqualTo(expected);
    }

    private static RecursiveComparisonConfiguration configuration(String... ignoredFields) {
        return RecursiveComparisonConfiguration.builder()
            .withIgnoreCollectionOrder(true)
            .withIgnoredFields(ignoredFields)
            .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
            .withComparatorForType(INSTANT_COMPARATOR, Instant.class)
            .withComparatorForType(LOCAL_DATE_TIME_COMPARATOR, LocalDateTime.class)
            .withComparatorForType(LOCAL_TIME_COMPARATOR, LocalTime.class)
            .build();
    }
}
