package com.lavrel.assistant.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The daily counters against a real PostgreSQL (schema from the Flyway migrations). Needs Docker: without it the
 * class is skipped (GitHub CI has Docker, so it always runs there). Each test rolls back.
 */
@DataJpaTest
@Import(AssistantUsageJdbcAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class AssistantUsageJdbcAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    private static final LocalDate DAY = LocalDate.of(2026, 11, 17);

    @Autowired
    private AssistantUsageJdbcAdapter usage;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void countsUpToTheLimitAndNoFurther() {
        assertThat(usage.tryConsume("user:7", DAY, 2)).isTrue();
        assertThat(usage.tryConsume("user:7", DAY, 2)).isTrue();
        assertThat(usage.tryConsume("user:7", DAY, 2)).isFalse();

        assertThat(usage.used("user:7", DAY)).isEqualTo(2);
    }

    @Test
    void releaseGivesOneBackSoTheNextAnswerFitsAgain() {
        usage.tryConsume("user:7", DAY, 2);
        usage.tryConsume("user:7", DAY, 2);

        usage.release("user:7", DAY);

        assertThat(usage.used("user:7", DAY)).isEqualTo(1);
        assertThat(usage.tryConsume("user:7", DAY, 2)).isTrue();
    }

    @Test
    void releaseNeverGoesBelowZero() {
        usage.tryConsume("user:7", DAY, 20);

        usage.release("user:7", DAY);
        usage.release("user:7", DAY);

        assertThat(usage.used("user:7", DAY)).isZero();
    }

    @Test
    void releaseWithoutACounterChangesNothing() {
        usage.release("user:8", DAY);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM assistant_usage WHERE scope = 'user:8'", Integer.class))
                .isZero();
    }

    @Test
    void releaseOnlyTouchesThatScopeAndDay() {
        usage.tryConsume("user:7", DAY, 20);
        usage.tryConsume("all", DAY, 400);
        usage.tryConsume("user:7", DAY.minusDays(1), 20);

        usage.release("user:7", DAY);

        assertThat(usage.used("user:7", DAY)).isZero();
        assertThat(usage.used("all", DAY)).isEqualTo(1);
        assertThat(usage.used("user:7", DAY.minusDays(1))).isEqualTo(1);
    }
}
