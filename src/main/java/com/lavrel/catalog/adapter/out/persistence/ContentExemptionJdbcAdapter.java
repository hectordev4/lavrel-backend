package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.application.port.out.ContentExemptionRepositoryPort;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Plain SQL on game.content_exempt (migration V14). The column is not mapped in GameJpaEntity on purpose:
 * saving a game through JPA never touches it, so a refresh cannot reset an exception by accident.
 */
@Component
public class ContentExemptionJdbcAdapter implements ContentExemptionRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public ContentExemptionJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void setExempt(long gameId, boolean exempt) {
        jdbc.update("UPDATE game SET content_exempt = :exempt WHERE id = :id",
                new MapSqlParameterSource().addValue("exempt", exempt).addValue("id", gameId));
    }

    @Override
    public Set<Long> findExemptGameIds(Collection<Long> gameIds) {
        if (gameIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList(
                "SELECT id FROM game WHERE content_exempt AND id IN (:ids)",
                Map.of("ids", gameIds), Long.class));
    }

    @Override
    public List<ExemptGame> findAll() {
        return jdbc.query("""
                SELECT g.id, g.title, gm.platform_game_id
                FROM game g
                LEFT JOIN game_mapping gm ON gm.game_id = g.id AND gm.platform = 'STEAM'
                WHERE g.content_exempt
                ORDER BY g.title
                """, Map.of(), (rs, i) -> new ExemptGame(rs.getLong("id"), rs.getString("title"),
                rs.getString("platform_game_id")));
    }
}
