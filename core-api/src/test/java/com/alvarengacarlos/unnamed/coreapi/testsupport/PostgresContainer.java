package com.alvarengacarlos.unnamed.coreapi.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throwaway Postgres in Docker for each Spring context and points the datasource to it. The image comes
 * from the postgres.image Maven property, the same as the jOOQ codegen.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainer {
    private static final String DEFAULT_IMAGE = "postgres:17";

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgreSQLContainer() {
        return new PostgreSQLContainer(DockerImageName.parse(System.getProperty("POSTGRES_IMAGE", DEFAULT_IMAGE)));
    }
}
