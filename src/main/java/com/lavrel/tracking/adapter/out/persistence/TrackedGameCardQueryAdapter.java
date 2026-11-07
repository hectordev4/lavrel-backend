package com.lavrel.tracking.adapter.out.persistence;

import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.NextAchievement;
import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.Sort;
import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.TrackedGameCard;
import com.lavrel.tracking.application.port.out.TrackedGameCardQueryPort;
import com.lavrel.tracking.domain.TrackingStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the tracking cards in two queries: one for the games (progress included), one for the "next
 * achievements" of all of them at once. Reads tables of other features on purpose (see the port).
 * The easiest locked achievements are the ones the largest share of the game's owners unlocked.
 */
@Component
public class TrackedGameCardQueryAdapter implements TrackedGameCardQueryPort {

    private static final int NEXT_ACHIEVEMENTS = 3;

    private static final String CARDS = """
            SELECT t.game_id, g.title, g.slug, g.cover_image_url, g.release_date, t.status, t.pin_position,
                   t.added_at, t.updated_at,
                   (SELECT COUNT(*) FROM achievement a WHERE a.game_id = t.game_id) AS total,
                   (SELECT COUNT(*) FROM user_achievement ua JOIN achievement a ON a.id = ua.achievement_id
                    WHERE ua.user_id = t.user_id AND a.game_id = t.game_id) AS unlocked
            FROM tracked_game t
            JOIN game g ON g.id = t.game_id
            WHERE t.user_id = :userId
            """;

    private static final String NEXT = """
            WITH owners AS (
                SELECT ag.game_id, COUNT(DISTINCT pa.user_id) AS n
                FROM account_game ag
                JOIN platform_account pa ON pa.id = ag.platform_account_id
                WHERE ag.game_id IN (:gameIds)
                GROUP BY ag.game_id
            ),
            unlocks AS (
                SELECT ua.achievement_id, COUNT(*) AS n
                FROM user_achievement ua
                JOIN achievement a ON a.id = ua.achievement_id
                WHERE a.game_id IN (:gameIds)
                GROUP BY ua.achievement_id
            ),
            ranked AS (
                SELECT a.game_id, a.id, a.name, a.icon_url, a.hidden,
                       CASE WHEN o.n > 0 THEN 100.0 * COALESCE(u.n, 0) / o.n END AS global_percent,
                       ROW_NUMBER() OVER (PARTITION BY a.game_id ORDER BY COALESCE(u.n, 0) DESC, a.id) AS rn
                FROM achievement a
                LEFT JOIN owners o ON o.game_id = a.game_id
                LEFT JOIN unlocks u ON u.achievement_id = a.id
                WHERE a.game_id IN (:gameIds)
                  AND NOT EXISTS (SELECT 1 FROM user_achievement mine
                                  WHERE mine.user_id = :userId AND mine.achievement_id = a.id)
            )
            SELECT game_id, id, name, icon_url, hidden, global_percent
            FROM ranked
            WHERE rn <= :limit
            ORDER BY game_id, rn
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public TrackedGameCardQueryAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackedGameCard> findCards(long userId, TrackingStatus status, Sort sort) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        String sql = CARDS;
        if (status != null) {
            sql += " AND t.status = :status";
            params.addValue("status", status.name());
        }
        return withNextAchievements(userId, jdbc.query(sql + orderBy(sort), params, TrackedGameCardQueryAdapter::row));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackedGameCard> findPinnedCards(long userId) {
        return withNextAchievements(userId, jdbc.query(CARDS + " AND t.pin_position IS NOT NULL ORDER BY t.pin_position",
                new MapSqlParameterSource("userId", userId), TrackedGameCardQueryAdapter::row));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackedGameCard> findCard(long userId, long gameId) {
        return withNextAchievements(userId, jdbc.query(CARDS + " AND t.game_id = :gameId",
                new MapSqlParameterSource().addValue("userId", userId).addValue("gameId", gameId),
                TrackedGameCardQueryAdapter::row));
    }

    private static String orderBy(Sort sort) {
        return switch (sort) {
            case TITLE -> " ORDER BY lower(g.title), t.game_id";
            case UPDATED -> " ORDER BY t.updated_at DESC, t.game_id";
            case RELEASE -> " ORDER BY g.release_date DESC NULLS LAST, lower(g.title), t.game_id";
        };
    }

    private List<TrackedGameCard> withNextAchievements(long userId, List<TrackedGameCard> cards) {
        if (cards.isEmpty()) {
            return cards;
        }
        Map<Long, List<NextAchievement>> next = new HashMap<>();
        jdbc.query(NEXT, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("gameIds", cards.stream().map(TrackedGameCard::gameId).toList())
                .addValue("limit", NEXT_ACHIEVEMENTS), (RowCallbackHandler) rs -> {
            double percent = rs.getDouble("global_percent");
            Double globalPercent = rs.wasNull() ? null : percent;
            next.computeIfAbsent(rs.getLong("game_id"), id -> new ArrayList<>())
                    .add(new NextAchievement(rs.getLong("id"), rs.getString("name"), rs.getString("icon_url"),
                            rs.getBoolean("hidden"), globalPercent));
        });
        return cards.stream().map(c -> new TrackedGameCard(c.gameId(), c.title(), c.slug(), c.coverImageUrl(), c.releaseDate(),
                c.status(), c.pinPosition(), c.unlocked(), c.total(), next.getOrDefault(c.gameId(), List.of()),
                c.addedAt(), c.updatedAt())).toList();
    }

    private static TrackedGameCard row(ResultSet rs, int i) throws SQLException {
        int pin = rs.getInt("pin_position");
        Integer pinPosition = rs.wasNull() ? null : pin;
        return new TrackedGameCard(rs.getLong("game_id"), rs.getString("title"), rs.getString("slug"),
                rs.getString("cover_image_url"),
                rs.getObject("release_date", LocalDate.class), TrackingStatus.valueOf(rs.getString("status")),
                pinPosition, rs.getInt("unlocked"), rs.getInt("total"), List.of(),
                rs.getTimestamp("added_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
