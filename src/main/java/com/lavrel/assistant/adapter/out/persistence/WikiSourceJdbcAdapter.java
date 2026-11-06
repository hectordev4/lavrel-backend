package com.lavrel.assistant.adapter.out.persistence;

import com.lavrel.assistant.application.port.out.WikiSourceRepositoryPort;
import com.lavrel.assistant.domain.WikiSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class WikiSourceJdbcAdapter implements WikiSourceRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public WikiSourceJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<WikiSource> find(long gameId) {
        return jdbc.query("SELECT game_id, api_url, license, added_at FROM assistant_wiki WHERE game_id = :gameId",
                new MapSqlParameterSource("gameId", gameId), WikiSourceJdbcAdapter::map).stream().findFirst();
    }

    @Override
    public List<WikiSource> findAll() {
        return jdbc.query("SELECT game_id, api_url, license, added_at FROM assistant_wiki ORDER BY game_id",
                new MapSqlParameterSource(), WikiSourceJdbcAdapter::map);
    }

    @Override
    public void save(WikiSource s) {
        jdbc.update("""
                INSERT INTO assistant_wiki (game_id, api_url, license, added_at)
                VALUES (:gameId, :apiUrl, :license, :addedAt)
                ON CONFLICT (game_id) DO UPDATE
                SET api_url = EXCLUDED.api_url, license = EXCLUDED.license, added_at = EXCLUDED.added_at
                """, new MapSqlParameterSource()
                .addValue("gameId", s.gameId())
                .addValue("apiUrl", s.apiUrl())
                .addValue("license", s.license())
                .addValue("addedAt", Timestamp.from(s.addedAt())));
    }

    @Override
    public boolean delete(long gameId) {
        return jdbc.update("DELETE FROM assistant_wiki WHERE game_id = :gameId",
                new MapSqlParameterSource("gameId", gameId)) > 0;
    }

    private static WikiSource map(ResultSet rs, int i) throws SQLException {
        return new WikiSource(rs.getLong("game_id"), rs.getString("api_url"), rs.getString("license"),
                rs.getTimestamp("added_at").toInstant());
    }
}
