package com.alvarengacarlos.unnamed.coreapi.testsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.alvarengacarlos.unnamed.coreapi.App;

/**
 * A repository test against a Postgres in Docker ({@link PostgresContainer}). The classes are named *RepositoryIT.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {App.class, PostgresContainer.class, Database.class, Fixtures.class},
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@ExtendWith({OrganizationContextExtension.class, DatabaseCleanerExtension.class})
@Tag("integration")
public @interface IntegrationTest {
}
