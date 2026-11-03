package com.lavrel.tracking.adapter.out.persistence;

import com.lavrel.tracking.application.port.out.TrackedGameRepositoryPort;
import com.lavrel.tracking.domain.TrackedGame;
import com.lavrel.tracking.domain.TrackingStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

/** Plain SQL on tracked_game (migration V15). */
@Component
public class TrackedGameJdbcAdapter implements TrackedGameRepositoryPort {

    private static final String COLUMNS = "user_id, game_id, status, pin_position, added_at, updated_at";

    private final NamedParameterJdbcTemplate jdbc;

    public TrackedGameJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<TrackedGame> find(long userId, long gameId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM tracked_game WHERE user_id = :userId AND game_id = :gameId",
                new MapSqlParameterSource().addValue("userId", userId).addValue("gameId", gameId),
                TrackedGameJdbcAdapter::map).stream().findFirst();
    }

    @Override
    public List<TrackedGame> findPinned(long userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM tracked_game "
                        + "WHERE user_id = :userId AND pin_position IS NOT NULL ORDER BY pin_position",
                new MapSqlParameterSource("userId", userId), TrackedGameJdbcAdapter::map);
    }

    @Override
    public void save(TrackedGame t) {
        jdbc.update(UPSERT, params(t));
    }

    @Override
    public void saveAll(List<TrackedGame> trackedGames) {
        if (!trackedGames.isEmpty()) {
            jdbc.batchUpdate(UPSERT, trackedGames.stream().map(TrackedGameJdbcAdapter::params)
                    .toArray(SqlParameterSource[]::new));
        }
    }

    @Override
    public boolean delete(long userId, long gameId) {
        return jdbc.update("DELETE FROM tracked_game WHERE user_id = :userId AND game_id = :gameId",
                new MapSqlParameterSource().addValue("userId", userId).addValue("gameId", gameId)) > 0;
    }

    private static final String UPSERT = """
            INSERT INTO tracked_game (user_id, game_id, status, pin_position, added_at, updated_at)
            VALUES (:userId, :gameId, :status, :pinPosition, :addedAt, :updatedAt)
            ON CONFLICT (user_id, game_id) DO UPDATE
            SET status = EXCLUDED.status, pin_position = EXCLUDED.pin_position, updated_at = EXCLUDED.updated_at
            """;

    private static SqlParameterSource params(TrackedGame t) {
        return new MapSqlParameterSource()
                .addValue("userId", t.userId())
                .addValue("gameId", t.gameId())
                .addValue("status", t.status().name())
                .addValue("pinPosition", t.pinPosition(), Types.SMALLINT)
                .addValue("addedAt", Timestamp.from(t.addedAt()))
                .addValue("updatedAt", Timestamp.from(t.updatedAt()));
    }

    private static TrackedGame map(ResultSet rs, int row) throws SQLException {
        int pin = rs.getInt("pin_position");
        Integer pinPosition = rs.wasNull() ? null : pin;
        return TrackedGame.restore(rs.getLong("user_id"), rs.getLong("game_id"),
                TrackingStatus.valueOf(rs.getString("status")), pinPosition,
                rs.getTimestamp("added_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
