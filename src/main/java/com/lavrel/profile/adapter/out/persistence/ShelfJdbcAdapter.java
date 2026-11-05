package com.lavrel.profile.adapter.out.persistence;

import com.lavrel.profile.application.port.out.ShelfRepositoryPort;
import com.lavrel.profile.domain.Shelf;
import com.lavrel.profile.domain.ShelfType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
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

/** Plain SQL on profile_shelf and profile_shelf_achievement (migration V23). */
@Component
public class ShelfJdbcAdapter implements ShelfRepositoryPort {

    private static final String COLUMNS =
            "id, public_id, user_id, type, title, position, collection_id, created_at";

    private final NamedParameterJdbcTemplate jdbc;

    public ShelfJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Shelf> findAll(long userId) {
        List<Row> rows = jdbc.query("SELECT " + COLUMNS + " FROM profile_shelf WHERE user_id = :userId "
                + "ORDER BY position, id", new MapSqlParameterSource("userId", userId), ShelfJdbcAdapter::row);
        return withAchievements(rows);
    }

    @Override
    public Optional<Shelf> find(long userId, UUID publicId) {
        List<Row> rows = jdbc.query("SELECT " + COLUMNS + " FROM profile_shelf "
                        + "WHERE user_id = :userId AND public_id = :publicId",
                new MapSqlParameterSource().addValue("userId", userId).addValue("publicId", publicId),
                ShelfJdbcAdapter::row);
        return withAchievements(rows).stream().findFirst();
    }

    @Override
    public Shelf save(Shelf s) {
        long id;
        if (s.id() == null) {
            id = jdbc.queryForObject("""
                    INSERT INTO profile_shelf (public_id, user_id, type, title, position, collection_id, created_at)
                    VALUES (:publicId, :userId, :type, :title, :position, :collectionId, :createdAt)
                    RETURNING id
                    """, params(s), Long.class);
        } else {
            id = s.id();
            jdbc.update("UPDATE profile_shelf SET title = :title, position = :position WHERE id = :id", params(s));
        }
        jdbc.update("DELETE FROM profile_shelf_achievement WHERE shelf_id = :id", new MapSqlParameterSource("id", id));
        List<Long> achievements = s.achievementIds();
        if (!achievements.isEmpty()) {
            SqlParameterSource[] batch = new SqlParameterSource[achievements.size()];
            for (int i = 0; i < achievements.size(); i++) {
                batch[i] = new MapSqlParameterSource().addValue("shelfId", id)
                        .addValue("achievementId", achievements.get(i)).addValue("position", i + 1);
            }
            jdbc.batchUpdate("INSERT INTO profile_shelf_achievement (shelf_id, achievement_id, position) "
                    + "VALUES (:shelfId, :achievementId, :position)", batch);
        }
        return Shelf.restore(id, s.publicId(), s.userId(), s.type(), s.title(), s.position(), s.collectionId(),
                achievements, s.createdAt());
    }

    @Override
    public void saveOrder(List<Shelf> shelves) {
        if (!shelves.isEmpty()) {
            jdbc.batchUpdate("UPDATE profile_shelf SET position = :position WHERE id = :id",
                    shelves.stream().map(ShelfJdbcAdapter::params).toArray(SqlParameterSource[]::new));
        }
    }

    @Override
    public void delete(long shelfId) {
        jdbc.update("DELETE FROM profile_shelf WHERE id = :id", new MapSqlParameterSource("id", shelfId));
    }

    private List<Shelf> withAchievements(List<Row> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Long>> byShelf = new HashMap<>();
        jdbc.query("SELECT shelf_id, achievement_id FROM profile_shelf_achievement WHERE shelf_id IN (:ids) "
                        + "ORDER BY shelf_id, position",
                new MapSqlParameterSource("ids", rows.stream().map(Row::id).toList()),
                (RowCallbackHandler) rs -> byShelf.computeIfAbsent(rs.getLong("shelf_id"), k -> new ArrayList<>())
                        .add(rs.getLong("achievement_id")));
        return rows.stream().map(r -> Shelf.restore(r.id(), r.publicId(), r.userId(), r.type(), r.title(),
                r.position(), r.collectionId(), byShelf.getOrDefault(r.id(), List.of()), r.createdAt().toInstant()))
                .toList();
    }

    private static SqlParameterSource params(Shelf s) {
        return new MapSqlParameterSource()
                .addValue("id", s.id())
                .addValue("publicId", s.publicId())
                .addValue("userId", s.userId())
                .addValue("type", s.type().name())
                .addValue("title", s.title())
                .addValue("position", s.position())
                .addValue("collectionId", s.collectionId())
                .addValue("createdAt", Timestamp.from(s.createdAt()));
    }

    private record Row(long id, UUID publicId, long userId, ShelfType type, String title, int position,
                       UUID collectionId, Timestamp createdAt) {
    }

    private static Row row(ResultSet rs, int i) throws SQLException {
        return new Row(rs.getLong("id"), rs.getObject("public_id", UUID.class), rs.getLong("user_id"),
                ShelfType.valueOf(rs.getString("type")), rs.getString("title"), rs.getInt("position"),
                rs.getObject("collection_id", UUID.class), rs.getTimestamp("created_at"));
    }
}
