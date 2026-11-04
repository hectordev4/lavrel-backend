package com.lavrel.library.adapter.out.persistence;

import com.lavrel.library.application.port.out.CollectionRepositoryPort;
import com.lavrel.library.domain.Collection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

/** Plain SQL on collection and collection_game (migration V17). */
@Component
public class CollectionJdbcAdapter implements CollectionRepositoryPort {

    private static final String COLUMNS = "id, public_id, user_id, name, position, created_at";

    private final NamedParameterJdbcTemplate jdbc;

    public CollectionJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Collection> findAll(long userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM collection WHERE user_id = :userId ORDER BY position, id",
                new MapSqlParameterSource("userId", userId), CollectionJdbcAdapter::map);
    }

    @Override
    public Optional<Collection> find(long userId, UUID publicId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM collection WHERE user_id = :userId AND public_id = :publicId",
                new MapSqlParameterSource().addValue("userId", userId).addValue("publicId", publicId),
                CollectionJdbcAdapter::map).stream().findFirst();
    }

    @Override
    public boolean nameTaken(long userId, String name, UUID exceptPublicId) {
        Boolean taken = jdbc.queryForObject("""
                        SELECT EXISTS (SELECT 1 FROM collection
                                       WHERE user_id = :userId AND lower(name) = lower(:name)
                                         AND (CAST(:except AS uuid) IS NULL OR public_id <> CAST(:except AS uuid)))
                        """,
                new MapSqlParameterSource().addValue("userId", userId).addValue("name", name)
                        .addValue("except", exceptPublicId), Boolean.class);
        return Boolean.TRUE.equals(taken);
    }

    @Override
    public Collection save(Collection c) {
        if (c.id() == null) {
            Long id = jdbc.queryForObject("""
                    INSERT INTO collection (public_id, user_id, name, position, created_at)
                    VALUES (:publicId, :userId, :name, :position, :createdAt)
                    RETURNING id
                    """, params(c), Long.class);
            return Collection.restore(id, c.publicId(), c.userId(), c.name(), c.position(), c.createdAt());
        }
        jdbc.update("UPDATE collection SET name = :name, position = :position WHERE id = :id", params(c));
        return c;
    }

    @Override
    public void saveAll(List<Collection> collections) {
        if (!collections.isEmpty()) {
            jdbc.batchUpdate("UPDATE collection SET name = :name, position = :position WHERE id = :id",
                    collections.stream().map(CollectionJdbcAdapter::params).toArray(SqlParameterSource[]::new));
        }
    }

    @Override
    public void delete(long collectionId) {
        jdbc.update("DELETE FROM collection WHERE id = :id", new MapSqlParameterSource("id", collectionId));
    }

    @Override
    public boolean addGame(long collectionId, long gameId, Instant at) {
        return jdbc.update("""
                INSERT INTO collection_game (collection_id, game_id, added_at) VALUES (:collectionId, :gameId, :at)
                ON CONFLICT (collection_id, game_id) DO NOTHING
                """, new MapSqlParameterSource().addValue("collectionId", collectionId).addValue("gameId", gameId)
                .addValue("at", Timestamp.from(at))) > 0;
    }

    @Override
    public boolean removeGame(long collectionId, long gameId) {
        return jdbc.update("DELETE FROM collection_game WHERE collection_id = :collectionId AND game_id = :gameId",
                new MapSqlParameterSource().addValue("collectionId", collectionId).addValue("gameId", gameId)) > 0;
    }

    @Override
    public Map<Long, Integer> visibleGameCounts(long userId) {
        Map<Long, Integer> counts = new HashMap<>();
        jdbc.query("""
                SELECT c.id, COUNT(g.id) AS games
                FROM collection c
                JOIN collection_game cg ON cg.collection_id = c.id
                JOIN game g ON g.id = cg.game_id AND g.enriched_at IS NOT NULL
                WHERE c.user_id = :userId
                  AND EXISTS (SELECT 1 FROM account_game ag JOIN platform_account pa ON pa.id = ag.platform_account_id
                              WHERE pa.user_id = :userId AND ag.game_id = g.id)
                GROUP BY c.id
                """, new MapSqlParameterSource("userId", userId),
                (RowCallbackHandler) rs -> counts.put(rs.getLong("id"), rs.getInt("games")));
        return counts;
    }

    private static SqlParameterSource params(Collection c) {
        return new MapSqlParameterSource()
                .addValue("id", c.id())
                .addValue("publicId", c.publicId())
                .addValue("userId", c.userId())
                .addValue("name", c.name())
                .addValue("position", c.position())
                .addValue("createdAt", Timestamp.from(c.createdAt()));
    }

    private static Collection map(ResultSet rs, int row) throws SQLException {
        return Collection.restore(rs.getLong("id"), rs.getObject("public_id", UUID.class), rs.getLong("user_id"),
                rs.getString("name"), rs.getInt("position"), rs.getTimestamp("created_at").toInstant());
    }
}
