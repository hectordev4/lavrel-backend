package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.application.port.out.GameSystemRepositoryPort;
import com.lavrel.catalog.domain.GameSystem;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GameSystemJdbcAdapter implements GameSystemRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public GameSystemJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Set<GameSystem> findByGame(long gameId) {
        Set<GameSystem> systems = EnumSet.noneOf(GameSystem.class);
        jdbc.query("SELECT system FROM game_system WHERE game_id = :gameId",
                new MapSqlParameterSource("gameId", gameId),
                (RowCallbackHandler) rs -> systems.add(GameSystem.valueOf(rs.getString("system"))));
        return systems;
    }

    @Override
    public void add(long gameId, GameSystem system, Source source) {
        jdbc.update("""
                INSERT INTO game_system (game_id, system, source) VALUES (:gameId, :system, :source)
                ON CONFLICT (game_id, system) DO NOTHING
                """, new MapSqlParameterSource().addValue("gameId", gameId).addValue("system", system.name())
                .addValue("source", source.name()));
    }

    @Override
    public boolean remove(long gameId, GameSystem system) {
        return jdbc.update("DELETE FROM game_system WHERE game_id = :gameId AND system = :system",
                new MapSqlParameterSource().addValue("gameId", gameId).addValue("system", system.name())) > 0;
    }
}
