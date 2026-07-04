package com.legalhelp.support;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MySQLContainer;

/**
 * Shared MySQL Testcontainers base for integration tests. Uses the Testcontainers
 * "singleton container" pattern (started once, never stopped) rather than a fresh
 * container per test class: started explicitly in a static initializer instead of via
 * {@code @Testcontainers}/{@code @Container}, so no per-class teardown/recreate cycle
 * happens. This avoids repeated container churn across the whole suite.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("legalhelp_test")
            .withUsername("legalhelp")
            .withPassword("legalhelp");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("app.jwt.secret", () -> "test_secret_at_least_32_characters_long");
        registry.add("app.encryption.key", () -> "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        registry.add("app.llm.api-key", () -> "test-key");
        registry.add("app.llm.model", () -> "claude-opus-4-8");
        // Tests drive TimeMeteringService.tickSession() directly for determinism —
        // the background @Scheduled tick must not run concurrently with test assertions.
        registry.add("app.chat.scheduled-metering-enabled", () -> "false");
    }
}
