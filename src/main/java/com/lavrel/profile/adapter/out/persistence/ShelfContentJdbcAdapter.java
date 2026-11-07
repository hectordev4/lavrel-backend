package com.lavrel.profile.adapter.out.persistence;

import com.lavrel.profile.application.port.in.ViewProfilesUseCase.AchievementItem;
import com.lavrel.profile.application.port.in.ViewProfilesUseCase.GameItem;
import com.lavrel.profile.application.port.in.ViewProfilesUseCase.Stats;
import com.lavrel.profile.application.port.out.ShelfContentQueryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One query per shelf. "Owned" means: in one of the user's linked accounts and published in the catalog, the same rule
 * as the library, so a profile never shows a game the user's own library hides.
 */
@Component
@Transactional(readOnly = true)
public class ShelfContentJdbcAdapter implements ShelfContentQueryPort {

    private static final String OWNED = """
            owned AS (
                SELECT ag.game_id,
                       MAX(ag.last_played_at) AS last_played
                FROM account_game ag
                JOIN platform_account pa ON pa.id = ag.platform_account_id
                JOIN game g ON g.id = ag.game_id AND g.enriched_at IS NOT NULL
                WHERE pa.user_id = :userId
                GROUP BY ag.game_id
            )""";

    private static final String PROGRESS = OWNED + """
            ,
            progress AS (
                SELECT o.game_id,
                       (SELECT COUNT(*) FROM achievement a WHERE a.game_id = o.game_id) AS total,
                       (SELECT COUNT(*) FROM user_achievement ua JOIN achievement a ON a.id = ua.achievement_id
                        WHERE ua.user_id = :userId AND a.game_id = o.game_id) AS unlocked,
                       (SELECT MAX(ua.unlocked_at) FROM user_achievement ua JOIN achievement a
                            ON a.id = ua.achievement_id
                        WHERE ua.user_id = :userId AND a.game_id = o.game_id) AS last_unlock
                FROM owned o
            )""";

    private final NamedParameterJdbcTemplate jdbc;

    public ShelfContentJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<CollectionContent> collection(long userId, UUID collectionId, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId)
                .addValue("collectionId", collectionId).addValue("limit", limit);
        List<String> names = jdbc.queryForList(
                "SELECT name FROM collection WHERE public_id = :collectionId AND user_id = :userId", params,
                String.class);
        if (names.isEmpty()) {
            return Optional.empty();
        }
        List<GameItem> games = jdbc.query("WITH " + OWNED + """
                SELECT g.id, g.title, g.slug, g.cover_image_url
                FROM collection c
                JOIN collection_game cg ON cg.collection_id = c.id
                JOIN owned o ON o.game_id = cg.game_id
                JOIN game g ON g.id = cg.game_id
                WHERE c.public_id = :collectionId AND c.user_id = :userId
                ORDER BY cg.added_at DESC, g.id
                LIMIT :limit
                """, params, ShelfContentJdbcAdapter::game);
        return Optional.of(new CollectionContent(names.get(0), games));
    }

    @Override
    public List<GameItem> recentlyPlayed(long userId, int limit) {
        return jdbc.query("WITH " + OWNED + """
                SELECT g.id, g.title, g.slug, g.cover_image_url
                FROM owned o JOIN game g ON g.id = o.game_id
                WHERE o.last_played IS NOT NULL
                ORDER BY o.last_played DESC, g.id
                LIMIT :limit
                """, params(userId, limit), ShelfContentJdbcAdapter::game);
    }

    @Override
    public List<GameItem> playing(long userId, int limit) {
        return jdbc.query("""
                SELECT g.id, g.title, g.slug, g.cover_image_url
                FROM tracked_game t JOIN game g ON g.id = t.game_id AND g.enriched_at IS NOT NULL
                WHERE t.user_id = :userId AND t.status = 'PLAYING'
                ORDER BY t.updated_at DESC, g.id
                LIMIT :limit
                """, params(userId, limit), ShelfContentJdbcAdapter::game);
    }

    @Override
    public List<GameItem> completed(long userId, int limit) {
        return jdbc.query("WITH " + PROGRESS + """
                SELECT g.id, g.title, g.slug, g.cover_image_url
                FROM progress p JOIN game g ON g.id = p.game_id
                WHERE p.total > 0 AND p.unlocked >= p.total
                ORDER BY p.last_unlock DESC NULLS LAST, g.id
                LIMIT :limit
                """, params(userId, limit), ShelfContentJdbcAdapter::game);
    }

    @Override
    public List<AchievementItem> achievements(long userId, List<Long> achievementIds) {
        if (achievementIds.isEmpty()) {
            return List.of();
        }
        List<AchievementItem> found = jdbc.query("""
                SELECT a.id, a.name, a.icon_url, g.id AS game_id, g.title AS game_title, g.slug AS game_slug, ua.unlocked_at,
                       CASE WHEN owners.n > 0
                            THEN 100.0 * (SELECT COUNT(*) FROM user_achievement x WHERE x.achievement_id = a.id)
                                 / owners.n END AS global_percent
                FROM achievement a
                JOIN game g ON g.id = a.game_id AND g.enriched_at IS NOT NULL
                JOIN user_achievement ua ON ua.achievement_id = a.id AND ua.user_id = :userId
                CROSS JOIN LATERAL (SELECT COUNT(DISTINCT pa.user_id) AS n
                                    FROM account_game ag JOIN platform_account pa ON pa.id = ag.platform_account_id
                                    WHERE ag.game_id = g.id) owners
                WHERE a.id IN (:ids)
                """, new MapSqlParameterSource().addValue("userId", userId).addValue("ids", achievementIds),
                (rs, i) -> {
                    double percent = rs.getDouble("global_percent");
                    Double globalPercent = rs.wasNull() ? null : Math.round(percent * 10.0) / 10.0;
                    Timestamp unlocked = rs.getTimestamp("unlocked_at");
                    return new AchievementItem(rs.getLong("id"), rs.getString("name"), rs.getString("icon_url"),
                            rs.getLong("game_id"), rs.getString("game_title"), rs.getString("game_slug"), globalPercent,
                            unlocked == null ? null : unlocked.toInstant());
                });
        Map<Long, AchievementItem> byId = found.stream()
                .collect(Collectors.toMap(AchievementItem::achievementId, Function.identity()));
        return achievementIds.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public Stats stats(long userId) {
        return jdbc.queryForObject("WITH " + PROGRESS + """
                SELECT (SELECT COUNT(*) FROM owned) AS owned,
                       (SELECT COUNT(*) FROM user_achievement WHERE user_id = :userId) AS unlocked,
                       (SELECT COUNT(*) FROM progress WHERE total > 0 AND unlocked >= total) AS completed
                """, new MapSqlParameterSource("userId", userId),
                (rs, i) -> new Stats(rs.getInt("owned"), rs.getInt("unlocked"), rs.getInt("completed")));
    }

    @Override
    public Set<Long> unlockedAmong(long userId, Collection<Long> achievementIds) {
        if (achievementIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList("""
                SELECT achievement_id FROM user_achievement WHERE user_id = :userId AND achievement_id IN (:ids)
                """, new MapSqlParameterSource().addValue("userId", userId).addValue("ids", achievementIds),
                Long.class));
    }

    private static MapSqlParameterSource params(long userId, int limit) {
        return new MapSqlParameterSource().addValue("userId", userId).addValue("limit", limit);
    }

    private static GameItem game(ResultSet rs, int i) throws SQLException {
        return new GameItem(rs.getLong("id"), rs.getString("title"), rs.getString("slug"), rs.getString("cover_image_url"));
    }
}
