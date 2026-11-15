package com.lavrel.progress.adapter.out.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The unlock SQL against a real PostgreSQL, with the schema built by the Flyway migrations. Needs Docker: without it
 * the class is skipped (GitHub CI has Docker, so it always runs there). Each test rolls back.
 */
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
class UserAchievementJpaRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    private static final Instant UNLOCKED = Instant.parse("2024-03-01T18:30:00Z");
    private static final Instant RECORDED = Instant.parse("2026-11-15T10:00:00Z");

    @Autowired
    private UserAchievementJpaRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    private long userId;
    private long firstAccount;
    private long secondAccount;
    private long achievementA;
    private long achievementB;

    @BeforeEach
    void someoneWithTwoSteamAccountsAndAGameWithTwoAchievements() {
        userId = id("INSERT INTO users (public_id, firebase_uid, username, role, created_at) "
                + "VALUES (1000000000000077, 'uid-unlocks', 'Tester', 'USER', now()) RETURNING id");
        firstAccount = id("INSERT INTO platform_account (user_id, platform, external_id) "
                + "VALUES (?, 'STEAM', '76561190000000001') RETURNING id", userId);
        secondAccount = id("INSERT INTO platform_account (user_id, platform, external_id) "
                + "VALUES (?, 'STEAM', '76561190000000002') RETURNING id", userId);
        long gameId = id("INSERT INTO game (title, slug) VALUES ('Unlock Test Game', 'unlock-test-game') RETURNING id");
        achievementA = id("INSERT INTO achievement (game_id, steam_achievement_id, name) "
                + "VALUES (?, 'ACH_A', 'A') RETURNING id", gameId);
        achievementB = id("INSERT INTO achievement (game_id, steam_achievement_id, name) "
                + "VALUES (?, 'ACH_B', 'B') RETURNING id", gameId);
    }

    @Test
    void theSameUnlockTwiceIsStoredOnce() {
        int first = repository.insertIfAbsent(userId, achievementA, firstAccount, UNLOCKED, RECORDED);
        int second = repository.insertIfAbsent(userId, achievementA, secondAccount, UNLOCKED.plusSeconds(60),
                RECORDED.plusSeconds(60));

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(rows()).isEqualTo(1);
        // The first row is kept as it was
        assertThat(jdbc.queryForObject(
                "SELECT source_account_id FROM user_achievement WHERE user_id = ? AND achievement_id = ?",
                Long.class, userId, achievementA)).isEqualTo(firstAccount);
        assertThat(jdbc.queryForObject(
                "SELECT unlocked_at FROM user_achievement WHERE user_id = ? AND achievement_id = ?",
                Timestamp.class, userId, achievementA).toInstant()).isEqualTo(UNLOCKED);
    }

    @Test
    void unlinkingAnAccountRemovesOnlyThatAccountsUnlocks() {
        repository.insertIfAbsent(userId, achievementA, firstAccount, UNLOCKED, RECORDED);
        repository.insertIfAbsent(userId, achievementB, secondAccount, UNLOCKED, RECORDED);

        int removed = repository.deleteBySourceAccount(userId, firstAccount);

        assertThat(removed).isEqualTo(1);
        assertThat(rows()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT achievement_id FROM user_achievement WHERE user_id = ?",
                Long.class, userId)).isEqualTo(achievementB);
    }

    private long id(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private int rows() {
        return jdbc.queryForObject("SELECT count(*) FROM user_achievement WHERE user_id = ?", Integer.class, userId);
    }
}
