package com.lavrel.library.adapter.out.persistence;

import com.lavrel.library.application.port.out.GameOwnershipPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** Reads account_game with platform_account (user feature table): the documented read-model exception. */
@Component
public class GameOwnershipJdbcAdapter implements GameOwnershipPort {

    private final NamedParameterJdbcTemplate jdbc;

    public GameOwnershipJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean owns(long userId, long gameId) {
        Boolean owns = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM account_game ag
                               JOIN platform_account pa ON pa.id = ag.platform_account_id
                               JOIN game g ON g.id = ag.game_id AND g.enriched_at IS NOT NULL
                               WHERE pa.user_id = :userId AND ag.game_id = :gameId)
                """, new MapSqlParameterSource().addValue("userId", userId).addValue("gameId", gameId), Boolean.class);
        return Boolean.TRUE.equals(owns);
    }
}
