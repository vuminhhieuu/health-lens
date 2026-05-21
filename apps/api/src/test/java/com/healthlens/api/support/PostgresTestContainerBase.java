package com.healthlens.api.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Postgres for integration tests. The container is started once per JVM so later
 * test classes do not hit {@link java.net.ConnectException} after JUnit stops a reused
 * {@code @Container} field from the parent class.
 */
public abstract class PostgresTestContainerBase {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = startPostgres();

    private static PostgreSQLContainer<?> startPostgres() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("healthlens_test")
                .withUsername("healthlens")
                .withPassword("healthlens_test_password");
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("app.storage.bucket-check-on-startup", () -> "false");
        registry.add("app.storage.cors-configure", () -> "false");
    }
}
