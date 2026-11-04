package com.lavrel.progress.adapter.out.persistence;

import com.lavrel.progress.application.port.out.GameAchievementsQueryPort;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GameAchievementsQueryAdapter implements GameAchievementsQueryPort {

    private static final String SQL = """
            SELECT a.id, a.name, a.description, a.icon_url, a.hidden, ua.unlocked_at
            FROM achievement a
            LEFT JOIN user_achievement ua ON ua.achievement_id = a.id AND ua.user_id = :userId
            WHERE a.game_id = :gameId
            ORDER BY a.id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public GameAchievementsQueryAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementRow> findAchievements(long gameId, Long userId) {
        return jdbc.query(SQL, new MapSqlParameterSource()
                .addValue("gameId", gameId)
                .addValue("userId", userId, Types.BIGINT), (rs, i) -> {
            Timestamp unlocked = rs.getTimestamp("unlocked_at");
            return new AchievementRow(rs.getLong("id"), rs.getString("name"), rs.getString("description"),
                    rs.getString("icon_url"), rs.getBoolean("hidden"), unlocked == null ? null : unlocked.toInstant());
        });
    }
}
