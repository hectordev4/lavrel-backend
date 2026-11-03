package com.lavrel.progress.adapter.out.persistence;

import java.sql.Types;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.lavrel.progress.application.port.out.AchievementStatsQueryPort;
import com.lavrel.progress.domain.AchievementStats;

/**
 * Read-only query adapter. It deliberately reads tables owned by other features
 * (users, platform_account, account_game, achievement, achievement_mapping): this is the
 * documented exception to the "talk through inbound ports" rule.
 *
 * Denominator: distinct users who own the game on a platform where the achievement is available
 * (Steam always counts; other platforms only when achievement_mapping.is_available).
 * Numerator: those users who have a user_achievement row.
 */
@Component
public class AchievementStatsQueryAdapter implements AchievementStatsQueryPort {

    private static final String SQL = """
            WITH owners AS (
                SELECT DISTINCT pa.user_id, pa.platform, u.country_id
                FROM account_game ag
                JOIN platform_account pa ON pa.id = ag.platform_account_id
                JOIN users u ON u.id = pa.user_id
                WHERE ag.game_id = :gameId
            ),
            eligible AS (
                SELECT a.id AS achievement_id, o.user_id, o.country_id
                FROM achievement a
                JOIN owners o ON (o.platform = 'STEAM' OR EXISTS (
                    SELECT 1 FROM achievement_mapping m
                    WHERE m.achievement_id = a.id AND m.platform = o.platform AND m.is_available))
                WHERE a.game_id = :gameId
                GROUP BY a.id, o.user_id, o.country_id
            )
            SELECT e.achievement_id,
                   100.0 * COUNT(ua.user_id) / COUNT(*) AS global_percent,
                   CASE
                       WHEN CAST(:countryId AS integer) IS NULL THEN NULL
                       WHEN COUNT(*) FILTER (WHERE e.country_id = CAST(:countryId AS integer)) >= :minSample
                       THEN 100.0 * COUNT(ua.user_id) FILTER (WHERE e.country_id = CAST(:countryId AS integer))
                            / NULLIF(COUNT(*) FILTER (WHERE e.country_id = CAST(:countryId AS integer)), 0)
                   END AS country_percent
            FROM eligible e
            LEFT JOIN user_achievement ua
                   ON ua.achievement_id = e.achievement_id AND ua.user_id = e.user_id
            GROUP BY e.achievement_id
            ORDER BY e.achievement_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public AchievementStatsQueryAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementStats> findGameStats(long gameId, Integer countryId, int minCountrySampleSize) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("gameId", gameId)
                .addValue("countryId", countryId, Types.INTEGER)
                .addValue("minSample", minCountrySampleSize);

        return jdbc.query(SQL, params, (rs, row) -> {
            double countryPercent = rs.getDouble("country_percent");
            boolean hasCountry = !rs.wasNull();
            return new AchievementStats(
                    rs.getLong("achievement_id"),
                    rs.getDouble("global_percent"),
                    hasCountry ? countryId : null,
                    hasCountry ? countryPercent : null);
        });
    }
}
