package com.lavrel.library.adapter.out.persistence;

import com.lavrel.catalog.adapter.out.persistence.GameFilterSql;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.PlayerMode;
import com.lavrel.library.application.port.in.GetLibraryUseCase.LibraryCriteria;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Builds the library SQL from the criteria; kept apart from the adapter so its shape can be unit tested. Every user
 * value is a bind parameter. Filters reuse GameFilterSql, so they mean exactly what they mean in search.
 */
final class LibrarySql {

    private static final String OWNED = """
            WITH owned AS (
                SELECT ag.game_id,
                       COALESCE(SUM(ag.playtime_minutes), 0) AS playtime,
                       MAX(ag.last_played_at) AS last_played,
                       string_agg(DISTINCT pa.platform, '|') AS owned_on
                FROM account_game ag
                JOIN platform_account pa ON pa.id = ag.platform_account_id
                WHERE pa.user_id = :userId
                GROUP BY ag.game_id
            )
            SELECT g.id, g.title, g.slug, g.cover_image_url, g.release_date, o.playtime, o.last_played, o.owned_on,
                   ach.total, mine.unlocked, t.status AS tracking_status, t.pin_position
            FROM owned o
            JOIN game g ON g.id = o.game_id
            LEFT JOIN LATERAL (SELECT COUNT(*) AS total FROM achievement a WHERE a.game_id = g.id) ach ON true
            LEFT JOIN LATERAL (SELECT COUNT(*) AS unlocked
                               FROM user_achievement ua JOIN achievement a ON a.id = ua.achievement_id
                               WHERE ua.user_id = :userId AND a.game_id = g.id) mine ON true
            LEFT JOIN tracked_game t ON t.user_id = :userId AND t.game_id = g.id
            """;

    private LibrarySql() {
    }

    record Query(String sql, MapSqlParameterSource params) {
    }

    /** One row more than the page size, so the adapter can tell whether there is a next page. */
    static Query build(long userId, LibraryCriteria c) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        StringBuilder sql = new StringBuilder(OWNED);
        List<String> where = new ArrayList<>();
        where.add("g.enriched_at IS NOT NULL");
        if (c.query() != null) {
            sql.append(GameFilterSql.QUERY_JOIN);
            params.addValue("q", c.query());
            where.add(GameFilterSql.NAME_MATCH);
        }
        for (int i = 0; i < c.tagIds().size(); i++) {
            String p = "tag" + i;
            where.add(GameFilterSql.tagCondition(p));
            params.addValue(p, c.tagIds().get(i));
        }
        int m = 0;
        for (PlayerMode mode : c.modes().stream().sorted().toList()) {
            String p = "mode" + m++;
            where.add(GameFilterSql.modeCondition(p));
            params.addValue(p, mode.featureIds());
        }
        addFilters(where, params, c);
        sql.append("WHERE ").append(String.join("\n  AND ", where)).append('\n');
        sql.append("ORDER BY ").append(orderBy(c)).append('\n');
        sql.append("LIMIT :limit OFFSET :offset");
        params.addValue("limit", c.size() + 1);
        params.addValue("offset", (long) c.page() * c.size());
        return new Query(sql.toString(), params);
    }

    /** Filters that only the library has: one of the user's collections. */
    private static void addFilters(List<String> where, MapSqlParameterSource params, LibraryCriteria c) {
        if (c.collectionId() != null) {
            where.add("""
                    EXISTS (SELECT 1 FROM collection_game cg JOIN collection c ON c.id = cg.collection_id
                            WHERE cg.game_id = g.id AND c.public_id = :collectionId AND c.user_id = :userId)""");
            params.addValue("collectionId", c.collectionId());
        }
    }

    private static String orderBy(LibraryCriteria c) {
        return switch (c.sort()) {
            case RECENT -> "o.last_played DESC NULLS LAST, g.search_name, g.id";
            case NAME -> "g.search_name, g.id";
            case PLAYTIME -> "o.playtime DESC, g.search_name, g.id";
            case PROGRESS -> "mine.unlocked::numeric / NULLIF(ach.total, 0) DESC NULLS LAST, g.search_name, g.id";
            case NEWEST -> "g.release_date DESC NULLS LAST, g.id";
        };
    }
}
