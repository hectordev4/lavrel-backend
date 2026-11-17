package com.lavrel.library.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavrel.library.domain.Collection;
import com.lavrel.library.domain.CollectionNameTakenException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The collection SQL against a real PostgreSQL, with the schema built by the Flyway migrations. Saving straight
 * through the adapter skips the service's nameTaken() check, which is exactly what two requests racing each other
 * do: only the unique index (uq_collection_user_name) is left to stop the second one. Needs Docker: without it the
 * class is skipped (GitHub CI has Docker, so it always runs there). Each test rolls back.
 */
@DataJpaTest
@Import(CollectionJdbcAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class CollectionJdbcAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");

    @Autowired
    private CollectionJdbcAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    private long userId;

    @BeforeEach
    void aUser() {
        userId = jdbc.queryForObject("INSERT INTO users (public_id, firebase_uid, username, role, created_at) "
                + "VALUES (1000000000000088, 'uid-collections', 'Tester', 'USER', now()) RETURNING id", Long.class);
    }

    @Test
    void creatingTheSameNameTwiceWithDifferentCapitalsIsANameClash() {
        adapter.save(Collection.create(userId, "Cozy", 1, NOW));

        assertThatThrownBy(() -> adapter.save(Collection.create(userId, "cozy", 2, NOW)))
                .isInstanceOf(CollectionNameTakenException.class);
    }

    @Test
    void renamingOntoAnExistingNameIsANameClash() {
        adapter.save(Collection.create(userId, "Cozy", 1, NOW));
        Collection other = adapter.save(Collection.create(userId, "Hard", 2, NOW));

        other.rename("COZY");

        assertThatThrownBy(() -> adapter.save(other)).isInstanceOf(CollectionNameTakenException.class);
    }

    @Test
    void anotherUserMayUseTheSameName() {
        long otherUser = jdbc.queryForObject("INSERT INTO users (public_id, firebase_uid, username, role, created_at) "
                + "VALUES (1000000000000089, 'uid-collections-2', 'Other', 'USER', now()) RETURNING id", Long.class);
        adapter.save(Collection.create(userId, "Cozy", 1, NOW));

        Collection theirs = adapter.save(Collection.create(otherUser, "Cozy", 1, NOW));

        assertThat(theirs.id()).isNotNull();
    }

    @Test
    void otherIntegrityErrorsAreNotTurnedIntoANameClash() {
        // No such user: a foreign key error, which must stay what it is
        assertThatThrownBy(() -> adapter.save(Collection.create(-1L, "Cozy", 1, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(CollectionNameTakenException.class);
    }
}
