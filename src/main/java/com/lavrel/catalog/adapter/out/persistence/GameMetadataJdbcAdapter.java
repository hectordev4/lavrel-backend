package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.application.port.out.GameMetadataRepositoryPort;
import com.lavrel.catalog.domain.GameMetadata;
import com.lavrel.catalog.domain.GameMetadata.AgeRating;
import com.lavrel.catalog.domain.GameMetadata.Creator;
import com.lavrel.catalog.domain.GameMetadata.Feature;
import com.lavrel.catalog.domain.GameMetadata.FeatureGroup;
import com.lavrel.catalog.domain.GameMetadata.Genre;
import com.lavrel.catalog.domain.GameMetadata.TagWeight;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plain SQL for the details tables (migration V9). It runs inside the caller's transaction, so a new game
 * and its details are committed together. Saving the details sets game.enriched_at: that is what publishes the game.
 */
@Component
public class GameMetadataJdbcAdapter implements GameMetadataRepositoryPort {

    private final NamedParameterJdbcTemplate named;
    private final JdbcOperations jdbc;

    public GameMetadataJdbcAdapter(NamedParameterJdbcTemplate named) {
        this.named = named;
        this.jdbc = named.getJdbcOperations();
    }

    @Override
    @Transactional
    public void save(long gameId, GameMetadata m) {
        jdbc.update("""
                UPDATE game
                SET release_date = ?, windows = ?, mac = ?, linux = ?, review_count = ?,
                    review_percent_positive = ?, is_free = ?, required_age = ?,
                    enriched_at = COALESCE(enriched_at, now()), refreshed_at = now()
                WHERE id = ?
                """, m.releaseDate(), m.windows(), m.mac(), m.linux(), m.reviewCount(),
                m.reviewPercentPositive(), m.free(), m.requiredAge(), gameId);

        jdbc.update("DELETE FROM game_tag WHERE game_id = ?", gameId);
        jdbc.update("DELETE FROM game_creator WHERE game_id = ?", gameId);
        jdbc.update("DELETE FROM game_genre WHERE game_id = ?", gameId);
        jdbc.update("DELETE FROM game_feature WHERE game_id = ?", gameId);
        jdbc.update("DELETE FROM game_age_rating WHERE game_id = ?", gameId);

        saveCreators(gameId, "DEVELOPER", m.developers());
        saveCreators(gameId, "PUBLISHER", m.publishers());
        saveCreators(gameId, "FRANCHISE", m.franchises());
        saveTags(gameId, m.tags());
        saveGenres(gameId, m.genres());
        saveFeatures(gameId, m.features());
        saveAgeRatings(gameId, m.ageRatings());
    }

    private void saveCreators(long gameId, String role, List<Creator> creators) {
        for (Creator creator : creators) {
            if (creator.name() == null || creator.name().isBlank()) {
                continue;
            }
            Long creatorId = jdbc.queryForObject("""
                    INSERT INTO creator (name, steam_creator_id) VALUES (?, ?)
                    ON CONFLICT (name) DO UPDATE
                    SET steam_creator_id = COALESCE(creator.steam_creator_id, EXCLUDED.steam_creator_id)
                    RETURNING id
                    """, Long.class, creator.name().trim(), creator.steamCreatorId());
            jdbc.update("INSERT INTO game_creator (game_id, creator_id, role) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                    gameId, creatorId, role);
        }
    }

    private void saveTags(long gameId, List<TagWeight> tags) {
        List<TagWeight> ranked = tags.stream()
                .sorted(Comparator.comparingInt(TagWeight::weight).reversed())
                .toList();
        List<Object[]> tagRows = new ArrayList<>();
        List<Object[]> gameTagRows = new ArrayList<>();
        for (int position = 0; position < ranked.size(); position++) {
            TagWeight tag = ranked.get(position);
            // A tag we have no name for yet gets a placeholder that the real name replaces later.
            tagRows.add(new Object[]{tag.tagId(), tag.name() != null ? tag.name() : "Tag " + tag.tagId()});
            gameTagRows.add(new Object[]{gameId, tag.tagId(), tag.weight(), position});
        }
        // Real names win over placeholders; a placeholder never overwrites a real name.
        jdbc.batchUpdate("""
                INSERT INTO tag (id, name) VALUES (?, ?)
                ON CONFLICT (id) DO UPDATE
                SET name = CASE WHEN EXCLUDED.name LIKE 'Tag %' THEN tag.name ELSE EXCLUDED.name END
                """, tagRows);
        jdbc.batchUpdate("""
                INSERT INTO game_tag (game_id, tag_id, weight, position) VALUES (?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """, gameTagRows);
    }

    /** A genre name that is already stored is kept: Steam renaming a genre does not change our data by itself. */
    private void saveGenres(long gameId, List<Genre> genres) {
        List<Object[]> genreRows = new ArrayList<>();
        List<Object[]> gameGenreRows = new ArrayList<>();
        for (Genre genre : genres) {
            genreRows.add(new Object[]{genre.id(), genre.name()});
            gameGenreRows.add(new Object[]{gameId, genre.id()});
        }
        jdbc.batchUpdate("INSERT INTO genre (id, name) VALUES (?, ?) ON CONFLICT (id) DO NOTHING", genreRows);
        jdbc.batchUpdate("INSERT INTO game_genre (game_id, genre_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                gameGenreRows);
    }

    private void saveFeatures(long gameId, List<Feature> features) {
        List<Object[]> featureRows = new ArrayList<>();
        List<Object[]> gameFeatureRows = new ArrayList<>();
        for (Feature feature : features) {
            featureRows.add(new Object[]{feature.id(),
                    feature.name() != null ? feature.name() : "Category " + feature.id(), feature.group().name()});
            gameFeatureRows.add(new Object[]{gameId, feature.id()});
        }
        jdbc.batchUpdate("""
                INSERT INTO feature (id, name, group_name) VALUES (?, ?, ?)
                ON CONFLICT (id) DO UPDATE
                SET name = CASE WHEN EXCLUDED.name LIKE 'Category %' THEN feature.name ELSE EXCLUDED.name END,
                    group_name = EXCLUDED.group_name
                """, featureRows);
        jdbc.batchUpdate("INSERT INTO game_feature (game_id, feature_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                gameFeatureRows);
    }

    private void saveAgeRatings(long gameId, List<AgeRating> ratings) {
        List<Object[]> rows = new ArrayList<>();
        for (AgeRating rating : ratings) {
            String descriptors = String.join("\n", rating.descriptors());
            rows.add(new Object[]{gameId, rating.board(), rating.rating(),
                    descriptors.length() > 500 ? descriptors.substring(0, 500) : descriptors});
        }
        jdbc.batchUpdate("""
                INSERT INTO game_age_rating (game_id, board, rating, descriptors) VALUES (?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameMetadata> find(long gameId) {
        List<GameMetadata> rows = jdbc.query("""
                SELECT release_date, windows, mac, linux, review_count, review_percent_positive,
                       is_free, required_age
                FROM game
                WHERE id = ? AND enriched_at IS NOT NULL
                """, (rs, i) -> new GameMetadata(
                rs.getObject("release_date", LocalDate.class),
                rs.getBoolean("windows"), rs.getBoolean("mac"), rs.getBoolean("linux"),
                nullableInt(rs, "review_count"), nullableInt(rs, "review_percent_positive"),
                rs.getBoolean("is_free"), nullableInt(rs, "required_age"),
                null, null, null, null, null, null, null), gameId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        GameMetadata base = rows.get(0);

        List<Creator> developers = new ArrayList<>();
        List<Creator> publishers = new ArrayList<>();
        List<Creator> franchises = new ArrayList<>();
        jdbc.query("""
                SELECT c.name, c.steam_creator_id, gc.role
                FROM game_creator gc JOIN creator c ON c.id = gc.creator_id
                WHERE gc.game_id = ?
                ORDER BY c.name
                """, rs -> {
            long steamId = rs.getLong("steam_creator_id");
            Creator creator = new Creator(rs.getString("name"), rs.wasNull() ? null : steamId);
            switch (rs.getString("role")) {
                case "DEVELOPER" -> developers.add(creator);
                case "PUBLISHER" -> publishers.add(creator);
                default -> franchises.add(creator);
            }
        }, gameId);

        List<TagWeight> tags = jdbc.query("""
                SELECT t.id, t.name, gt.weight
                FROM game_tag gt JOIN tag t ON t.id = gt.tag_id
                WHERE gt.game_id = ?
                ORDER BY gt.position
                """, (rs, i) -> new TagWeight(rs.getLong("id"), rs.getString("name"), rs.getInt("weight")), gameId);

        List<Genre> genres = jdbc.query("""
                SELECT g.id, g.name
                FROM game_genre gg JOIN genre g ON g.id = gg.genre_id
                WHERE gg.game_id = ?
                ORDER BY g.name
                """, (rs, i) -> new Genre(rs.getInt("id"), rs.getString("name")), gameId);

        List<Feature> features = jdbc.query("""
                SELECT f.id, f.name, f.group_name
                FROM game_feature gf JOIN feature f ON f.id = gf.feature_id
                WHERE gf.game_id = ?
                ORDER BY f.id
                """, (rs, i) -> new Feature(rs.getInt("id"), rs.getString("name"),
                FeatureGroup.valueOf(rs.getString("group_name"))), gameId);

        List<AgeRating> ratings = jdbc.query("""
                SELECT board, rating, descriptors FROM game_age_rating WHERE game_id = ? ORDER BY board
                """, (rs, i) -> {
            String descriptors = rs.getString("descriptors");
            List<String> lines = descriptors == null || descriptors.isBlank()
                    ? List.of() : Arrays.asList(descriptors.split("\n"));
            return new AgeRating(rs.getString("board"), rs.getString("rating"), lines);
        }, gameId);

        return Optional.of(new GameMetadata(base.releaseDate(), base.windows(), base.mac(), base.linux(),
                base.reviewCount(), base.reviewPercentPositive(), base.free(), base.requiredAge(),
                developers, publishers, franchises, tags, genres, features, ratings));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findPendingGameIds(int limit) {
        return jdbc.queryForList("SELECT id FROM game WHERE enriched_at IS NULL ORDER BY id LIMIT ?",
                Long.class, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public int countPending() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM game WHERE enriched_at IS NULL", Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findStaleGameIds(Instant olderThan, int limit) {
        return jdbc.queryForList("""
                SELECT id FROM game
                WHERE enriched_at IS NOT NULL AND (refreshed_at IS NULL OR refreshed_at < ?)
                ORDER BY refreshed_at NULLS FIRST, id
                LIMIT ?
                """, Long.class, Timestamp.from(olderThan), limit);
    }

    @Override
    @Transactional(readOnly = true)
    public int countStale(Instant olderThan) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM game
                WHERE enriched_at IS NOT NULL AND (refreshed_at IS NULL OR refreshed_at < ?)
                """, Integer.class, Timestamp.from(olderThan));
        return count == null ? 0 : count;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findKnownTagIds(Collection<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(named.queryForList("SELECT id FROM tag WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", tagIds), Long.class));
    }

    @Override
    @Transactional
    public void saveTagNames(Map<Long, String> names, boolean overwrite) {
        List<Object[]> rows = new ArrayList<>();
        names.forEach((id, name) -> rows.add(new Object[]{id, name.length() > 100 ? name.substring(0, 100) : name}));
        // Without overwrite only placeholders ("Tag 123") are replaced: names we already have never change by themselves.
        String onConflict = overwrite
                ? "DO UPDATE SET name = EXCLUDED.name"
                : "DO UPDATE SET name = CASE WHEN tag.name LIKE 'Tag %' THEN EXCLUDED.name ELSE tag.name END";
        jdbc.batchUpdate("INSERT INTO tag (id, name) VALUES (?, ?) ON CONFLICT (id) " + onConflict, rows);
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
