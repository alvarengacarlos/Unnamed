package com.alvarengacarlos.unnamed.coreapi.testsupport;

import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.CLAZZES;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.GRADINGS;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.GRADING_CANDIDATES;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.MEMBERS;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.RANKS;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.RANK_CATEGORIES;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.STUDENTS;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.STUDENT_ENROLLMENTS;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.TEACHERS;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.TOURNAMENTS;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.TOURNAMENT_COMPETITORS;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.TOURNAMENT_MATCHES;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.TUITIONS;
import static com.alvarengacarlos.unnamed.coreapi.jooq.Tables.TUITION_PAYMENTS;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class Database {
    private static final List<Table<?>> TABLES_IN_TRUNCATE_ORDER = List.of(
        TOURNAMENT_MATCHES,
        TOURNAMENT_COMPETITORS,
        TOURNAMENTS,
        GRADING_CANDIDATES,
        GRADINGS,
        TUITION_PAYMENTS,
        STUDENT_ENROLLMENTS,
        CLAZZES,
        STUDENTS,
        TUITIONS,
        TEACHERS,
        MEMBERS,
        RANKS,
        RANK_CATEGORIES);

    @Autowired
    private DSLContext dsl;

    public void clean() {
        TABLES_IN_TRUNCATE_ORDER.forEach(table -> dsl.truncate(table)
            .cascade()
            .execute());
    }
}
