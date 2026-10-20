package com.lavrel.devtools;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DEVELOPMENT ONLY. Fills the database with fake players so the statistics (global and by country)
 * can be tried without real users. It is plain set-based SQL on purpose: it is test tooling,
 * not part of the application's domain, and it is only loaded when app.dev.fake-data-enabled=true.
 *
 * Fake users are recognisable by the firebase_uid prefix "fake-", so they can be removed in one go.
 * They own random games from the CURRENT catalog, so seed the catalog first.
 */
@Service
@ConditionalOnProperty(name = "app.dev.fake-data-enabled", havingValue = "true")
public class FakeDataService {

    /** Users with a random country (10% without one), each with a verified Steam account. */
    private static final String INSERT_USERS_AND_ACCOUNTS = """
            WITH c AS (SELECT array_agg(id ORDER BY id) AS ids FROM country),
            new_users AS (
                INSERT INTO users (firebase_uid, username, email, country_id, created_at)
                SELECT 'fake-' || gen_random_uuid(),
                       'fake_' || substr(replace(gen_random_uuid()::text, '-', ''), 1, 12),
                       NULL,
                       CASE WHEN random() < 0.1 THEN NULL
                            ELSE c.ids[1 + floor(random() * cardinality(c.ids))::int] END,
                       now() - random() * interval '365 days'
                FROM generate_series(1, ?::int) AS gs, c
                RETURNING id, username
            )
            INSERT INTO platform_account (user_id, platform, external_id, display_name, verified, verified_at, linked_at)
            SELECT id, 'STEAM', '76561199' || lpad(id::text, 9, '0'), username, true, now(), now()
            FROM new_users
            """;

    /** Each fake account without games gets N random catalog games with random playtime. */
    private static final String INSERT_ACCOUNT_GAMES = """
            INSERT INTO account_game (platform_account_id, game_id, playtime_minutes, last_played_at, last_synced_at)
            SELECT pa.id, g.id, floor(random() * 6000)::int, now() - random() * interval '400 days', now()
            FROM platform_account pa
            JOIN users u ON u.id = pa.user_id AND u.firebase_uid LIKE 'fake-%'
            CROSS JOIN LATERAL (
                SELECT id FROM game WHERE pa.id IS NOT NULL ORDER BY random() LIMIT ?::int
            ) g
            WHERE NOT EXISTS (SELECT 1 FROM account_game ag WHERE ag.platform_account_id = pa.id)
            """;

    /**
     * Every fake account/game pair gets a random "completion" (0..1); an achievement is unlocked when a
     * random draw falls under completion scaled by a fixed per-achievement rarity, so global percentages
     * differ between achievements instead of all being equal.
     */
    private static final String INSERT_UNLOCKS = """
            WITH completion AS MATERIALIZED (
                SELECT platform_account_id, game_id, random() AS rate FROM account_game
            )
            INSERT INTO user_achievement (user_id, achievement_id, source_account_id, unlocked_at, recorded_at)
            SELECT pa.user_id, a.id, pa.id, now() - random() * interval '300 days', now()
            FROM account_game ag
            JOIN platform_account pa ON pa.id = ag.platform_account_id
            JOIN users u ON u.id = pa.user_id AND u.firebase_uid LIKE 'fake-%'
            JOIN completion c ON c.platform_account_id = ag.platform_account_id AND c.game_id = ag.game_id
            JOIN achievement a ON a.game_id = ag.game_id
            WHERE random() < c.rate * (0.3 + 0.7 * ((a.id * 37) % 100) / 100.0)::float8
            ON CONFLICT (user_id, achievement_id) DO NOTHING
            """;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public FakeDataService(JdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    public Summary create(int users, int gamesPerUser) {
        Integer gamesInCatalog = jdbc.queryForObject("SELECT count(*) FROM game", Integer.class);
        Summary summary = tx.execute(status -> {
            int usersCreated = jdbc.update(INSERT_USERS_AND_ACCOUNTS, users);
            int gamesAdded = jdbc.update(INSERT_ACCOUNT_GAMES, gamesPerUser);
            int unlocksRecorded = jdbc.update(INSERT_UNLOCKS);
            return new Summary(usersCreated, gamesAdded, unlocksRecorded, gamesInCatalog == null ? 0 : gamesInCatalog);
        });
        return summary;
    }

    /** Deleting the users cascades to their accounts, owned games and unlocks. */
    public int deleteAll() {
        Integer deleted = tx.execute(status -> jdbc.update("DELETE FROM users WHERE firebase_uid LIKE 'fake-%'"));
        return deleted == null ? 0 : deleted;
    }

    public record Summary(int usersCreated, int gamesAdded, int unlocksRecorded, int gamesInCatalog) {
    }
}
