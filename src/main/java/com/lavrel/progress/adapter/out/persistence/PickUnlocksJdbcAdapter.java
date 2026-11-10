package com.lavrel.progress.adapter.out.persistence;

import com.lavrel.progress.application.port.in.PickUnlocksUseCase.AchievementSort;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.GameSort;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.GameWithUnlocks;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.UnlockedAchievement;
import com.lavrel.progress.application.port.out.PickUnlocksQueryPort;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Only fixed SQL fragments are inserted (the sort orders, in place of ORDER_BY); every user value is a bind
 * parameter. Not String.format: the SQL contains '%' for LIKE, which format would read as a placeholder.
 */
@Component
@Transactional(readOnly = true)
public class PickUnlocksJdbcAdapter implements PickUnlocksQueryPort {

    private static final String GAMES = """
            SELECT g.id, g.title, g.slug, g.cover_image_url,
                   COUNT(*) AS unlocked,
                   (SELECT COUNT(*) FROM achievement a2 WHERE a2.game_id = g.id) AS total,
                   MAX(ua.unlocked_at) AS last_unlock
            FROM user_achievement ua
            JOIN achievement a ON a.id = ua.achievement_id
            JOIN game g ON g.id = a.game_id AND g.enriched_at IS NOT NULL
            WHERE ua.user_id = :userId
              AND (CAST(:q AS text) IS NULL OR g.search_name LIKE '%' || lavrel_normalize(:q) || '%')
            GROUP BY g.id, g.title, g.slug, g.cover_image_url, g.search_name
            ORDER BY ORDER_BY
            LIMIT :limit OFFSET :offset
            """;

    private static final String UNLOCKED = """
            SELECT a.id, a.name, a.icon_url, a.hidden, ua.unlocked_at,
                   CASE WHEN owners.n > 0
                        THEN round(100.0 * (SELECT COUNT(*) FROM user_achievement x WHERE x.achievement_id = a.id)
                                   / owners.n, 1) END AS global_percent
            FROM user_achievement ua
            JOIN achievement a ON a.id = ua.achievement_id
            JOIN game g ON g.id = a.game_id AND g.enriched_at IS NOT NULL
            CROSS JOIN (SELECT COUNT(DISTINCT pa.user_id) AS n
                        FROM account_game ag JOIN platform_account pa ON pa.id = ag.platform_account_id
                        WHERE ag.game_id = :gameId) owners
            WHERE ua.user_id = :userId AND a.game_id = :gameId
            ORDER BY ORDER_BY
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public PickUnlocksJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static String gamesSql(GameSort sort) {
        return GAMES.replace("ORDER_BY", sort == GameSort.NAME ? "g.search_name, g.id"
                : "last_unlock DESC NULLS LAST, g.id");
    }

    static String unlockedSql(AchievementSort sort) {
        return UNLOCKED.replace("ORDER_BY", sort == AchievementSort.DATE
                ? "ua.unlocked_at DESC NULLS LAST, a.id"
                : "global_percent ASC NULLS LAST, ua.unlocked_at DESC NULLS LAST, a.id");
    }

    @Override
    public List<GameWithUnlocks> gamesWithUnlocks(long userId, String query, GameSort sort, int limit, long offset) {
        return jdbc.query(gamesSql(sort), new MapSqlParameterSource()
                .addValue("userId", userId).addValue("q", query).addValue("limit", limit)
                .addValue("offset", offset), (rs, i) -> {
            Timestamp last = rs.getTimestamp("last_unlock");
            return new GameWithUnlocks(rs.getLong("id"), rs.getString("title"), rs.getString("slug"),
                    rs.getString("cover_image_url"), rs.getInt("unlocked"), rs.getInt("total"),
                    last == null ? null : last.toInstant());
        });
    }

    @Override
    public List<UnlockedAchievement> unlockedIn(long userId, long gameId, AchievementSort sort) {
        return jdbc.query(unlockedSql(sort), new MapSqlParameterSource()
                .addValue("userId", userId).addValue("gameId", gameId), (rs, i) -> {
            double percent = rs.getDouble("global_percent");
            Double globalPercent = rs.wasNull() ? null : percent;
            Timestamp unlocked = rs.getTimestamp("unlocked_at");
            return new UnlockedAchievement(rs.getLong("id"), rs.getString("name"), rs.getString("icon_url"),
                    rs.getBoolean("hidden"), globalPercent, unlocked == null ? null : unlocked.toInstant());
        });
    }
}
