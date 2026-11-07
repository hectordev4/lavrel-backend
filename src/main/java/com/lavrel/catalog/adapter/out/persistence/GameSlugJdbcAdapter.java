package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.application.port.out.GameSlugPort;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GameSlugJdbcAdapter implements GameSlugPort {

    private final NamedParameterJdbcTemplate jdbc;

    public GameSlugJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> slugOf(long gameId) {
        return jdbc.queryForList("SELECT slug FROM game WHERE id = :id", new MapSqlParameterSource("id", gameId),
                String.class).stream().findFirst();
    }

    @Override
    public Optional<Long> gameIdOf(String slug) {
        return jdbc.queryForList("SELECT id FROM game WHERE slug = :slug", new MapSqlParameterSource("slug", slug),
                Long.class).stream().findFirst();
    }
}
