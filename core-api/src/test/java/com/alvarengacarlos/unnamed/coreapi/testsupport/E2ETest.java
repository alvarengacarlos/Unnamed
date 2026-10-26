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
 * A test that calls the API over HTTP. The data it needs may be created directly in the database with
 * {@link Fixtures}. The classes are named *ApiIT.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {App.class, PostgresContainer.class, ApiClient.class, Database.class, Fixtures.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith({JwksExtension.class, OrganizationContextExtension.class, DatabaseCleanerExtension.class})
@Tag("e2e")
public @interface E2ETest {
}
