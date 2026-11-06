package com.lavrel.assistant.adapter.out.persistence;

import com.lavrel.assistant.application.port.out.AchievementContextPort;
import com.lavrel.assistant.domain.AchievementContext;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AchievementContextJdbcAdapter implements AchievementContextPort {

    private final NamedParameterJdbcTemplate jdbc;

    public AchievementContextJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<AchievementContext> find(long gameId, long achievementId) {
        return jdbc.query("""
                        SELECT g.id AS game_id, g.title, a.id, a.name, a.description, a.hidden
                        FROM achievement a
                        JOIN game g ON g.id = a.game_id AND g.enriched_at IS NOT NULL
                        WHERE a.id = :achievementId AND a.game_id = :gameId
                        """, new MapSqlParameterSource().addValue("gameId", gameId)
                        .addValue("achievementId", achievementId),
                (rs, i) -> new AchievementContext(rs.getLong("game_id"), rs.getString("title"), rs.getLong("id"),
                        rs.getString("name"), rs.getString("description"), rs.getBoolean("hidden")))
                .stream().findFirst();
    }
}
