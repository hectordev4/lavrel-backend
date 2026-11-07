package com.lavrel.progress.adapter.out.persistence;

import com.lavrel.progress.application.port.in.GetMyProgressUseCase.MyStats;
import com.lavrel.progress.application.port.in.GetMyProgressUseCase.RecentUnlock;
import com.lavrel.progress.application.port.out.MyProgressQueryPort;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Same rules as the library and the profile shelves: owned means in a linked account and published. */
@Component
@Transactional(readOnly = true)
public class MyProgressJdbcAdapter implements MyProgressQueryPort {

    private static final String STATS = """
            WITH owned AS (
                SELECT DISTINCT ag.game_id
                FROM account_game ag
                JOIN platform_account pa ON pa.id = ag.platform_account_id
                JOIN game g ON g.id = ag.game_id AND g.enriched_at IS NOT NULL
                WHERE pa.user_id = :userId
            ),
            progress AS (
                SELECT o.game_id,
                       (SELECT COUNT(*) FROM achievement a WHERE a.game_id = o.game_id) AS total,
                       (SELECT COUNT(*) FROM user_achievement ua JOIN achievement a ON a.id = ua.achievement_id
                        WHERE ua.user_id = :userId AND a.game_id = o.game_id) AS unlocked
                FROM owned o
            )
            SELECT (SELECT COUNT(*) FROM user_achievement ua
                    JOIN achievement a ON a.id = ua.achievement_id
                    JOIN game g ON g.id = a.game_id AND g.enriched_at IS NOT NULL
                    WHERE ua.user_id = :userId) AS achievements_unlocked,
                   (SELECT round(avg(100.0 * unlocked / total), 1) FROM progress
                    WHERE total > 0 AND unlocked > 0) AS average_completion,
                   (SELECT COUNT(*) FROM progress WHERE total > 0 AND unlocked >= total) AS perfect_games,
                   (SELECT COUNT(*) FROM owned) AS games_owned
            """;

    private static final String RECENT = """
            SELECT a.id, a.name, a.icon_url, a.hidden, g.id AS game_id, g.title AS game_title, g.slug AS game_slug,
                   ua.unlocked_at,
                   CASE WHEN owners.n > 0
                        THEN round(100.0 * (SELECT COUNT(*) FROM user_achievement x WHERE x.achievement_id = a.id)
                                   / owners.n, 1) END AS global_percent
            FROM user_achievement ua
            JOIN achievement a ON a.id = ua.achievement_id
            JOIN game g ON g.id = a.game_id AND g.enriched_at IS NOT NULL
            CROSS JOIN LATERAL (SELECT COUNT(DISTINCT pa.user_id) AS n
                                FROM account_game ag JOIN platform_account pa ON pa.id = ag.platform_account_id
                                WHERE ag.game_id = g.id) owners
            WHERE ua.user_id = :userId
            ORDER BY ua.unlocked_at DESC NULLS LAST, a.id DESC
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public MyProgressJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public MyStats stats(long userId) {
        return jdbc.queryForObject(STATS, new MapSqlParameterSource("userId", userId), (rs, i) -> {
            double average = rs.getDouble("average_completion");
            Double averageCompletion = rs.wasNull() ? null : average;
            return new MyStats(rs.getInt("achievements_unlocked"), averageCompletion, rs.getInt("perfect_games"),
                    rs.getInt("games_owned"));
        });
    }

    @Override
    public List<RecentUnlock> recentUnlocks(long userId, int limit) {
        return jdbc.query(RECENT, new MapSqlParameterSource().addValue("userId", userId).addValue("limit", limit),
                (rs, i) -> {
                    double percent = rs.getDouble("global_percent");
                    Double globalPercent = rs.wasNull() ? null : percent;
                    Timestamp unlocked = rs.getTimestamp("unlocked_at");
                    return new RecentUnlock(rs.getLong("id"), rs.getString("name"), rs.getString("icon_url"),
                            rs.getBoolean("hidden"), rs.getLong("game_id"), rs.getString("game_title"),
                            rs.getString("game_slug"), globalPercent, unlocked == null ? null : unlocked.toInstant());
                });
    }
}
