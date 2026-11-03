package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GameSearchCriteria;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.PlayerMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Builds the search SQL from the criteria. Kept apart from the adapter so the query shape can be unit tested.
 * Every user value goes in as a bind parameter; only fixed fragments are concatenated.
 *
 * Name matching uses lavrel_normalize (lower case, no accents, punctuation as spaces) on both sides and pg_trgm,
 * so "witcer 3" finds "The Witcher® 3: Wild Hunt". A tag counts for a game only when it really describes it:
 * among the game's top {@value #TAG_TOP_POSITIONS} tags, or holding at least a quarter of its top tag's weight
 * (the lower Steam tags are noise).
 */
final class GameSearchSql {

    static final int TAG_TOP_POSITIONS = 10;
    static final int RATING_MIN_REVIEWS = 100;

    private static final String SELECT = """
            SELECT g.id, g.title, g.cover_image_url, g.release_date, g.windows, g.mac, g.linux,
                   g.review_count, g.review_percent_positive,
                   (SELECT string_agg(t.name, '|' ORDER BY gt.position)
                    FROM game_tag gt JOIN tag t ON t.id = gt.tag_id
                    WHERE gt.game_id = g.id AND gt.position < 3) AS top_tags,
                   EXISTS (SELECT 1 FROM game_feature gf WHERE gf.game_id = g.id AND gf.feature_id IN (%s)) AS single_player,
                   EXISTS (SELECT 1 FROM game_feature gf WHERE gf.game_id = g.id AND gf.feature_id IN (%s)) AS co_op,
                   EXISTS (SELECT 1 FROM game_feature gf WHERE gf.game_id = g.id AND gf.feature_id IN (%s)) AS multiplayer
            FROM game g
            """.formatted(ids(PlayerMode.SINGLE_PLAYER), ids(PlayerMode.CO_OP), ids(PlayerMode.MULTIPLAYER));

    private GameSearchSql() {
    }

    record Query(String sql, MapSqlParameterSource params) {
    }

    /** Fetches one row more than the page size, so the adapter can tell whether there is a next page. */
    static Query build(GameSearchCriteria c) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder(SELECT);
        if (c.query() != null) {
            sql.append("CROSS JOIN (SELECT lavrel_normalize(:q) AS v) q\n");
            params.addValue("q", c.query());
        }
        List<String> where = new ArrayList<>();
        where.add("g.enriched_at IS NOT NULL");
        if (c.query() != null) {
            where.add("(g.search_name LIKE '%' || q.v || '%' OR q.v <% g.search_name)");
        }
        for (int i = 0; i < c.tagIds().size(); i++) {
            String p = "tag" + i;
            where.add("""
                    EXISTS (SELECT 1 FROM game_tag gt WHERE gt.game_id = g.id AND gt.tag_id = :%s
                            AND (gt.position < %d
                                 OR gt.weight * 4 >= (SELECT max(t0.weight) FROM game_tag t0 WHERE t0.game_id = g.id)))"""
                    .formatted(p, TAG_TOP_POSITIONS));
            params.addValue(p, c.tagIds().get(i));
        }
        creator(where, params, "franchiseId", c.franchiseId(), "FRANCHISE");
        creator(where, params, "developerId", c.developerId(), "DEVELOPER");
        creator(where, params, "publisherId", c.publisherId(), "PUBLISHER");
        int m = 0;
        for (PlayerMode mode : c.modes().stream().sorted().toList()) {
            String p = "mode" + m++;
            where.add("EXISTS (SELECT 1 FROM game_feature gf WHERE gf.game_id = g.id AND gf.feature_id IN (:" + p + "))");
            params.addValue(p, mode.featureIds());
        }
        sql.append("WHERE ").append(String.join("\n  AND ", where)).append('\n');
        sql.append("ORDER BY ").append(orderBy(c)).append('\n');
        sql.append("LIMIT :limit OFFSET :offset");
        params.addValue("limit", c.size() + 1);
        params.addValue("offset", (long) c.page() * c.size());
        return new Query(sql.toString(), params);
    }

    private static void creator(List<String> where, MapSqlParameterSource params, String param, Long id, String role) {
        if (id != null) {
            where.add("EXISTS (SELECT 1 FROM game_creator gc WHERE gc.game_id = g.id AND gc.creator_id = :" + param
                    + " AND gc.role = '" + role + "')");
            params.addValue(param, id);
        }
    }

    private static String orderBy(GameSearchCriteria c) {
        return switch (c.sort()) {
            case RELEVANCE -> """
                    CASE WHEN g.search_name = q.v THEN 0
                         WHEN g.search_name LIKE q.v || '%' THEN 1
                         WHEN g.search_name LIKE '%' || q.v || '%' THEN 2
                         ELSE 3 END,
                    word_similarity(q.v, g.search_name) DESC, g.review_count DESC NULLS LAST, g.id""";
            case POPULAR -> "g.review_count DESC NULLS LAST, g.id";
            case RATING -> "(COALESCE(g.review_count, 0) >= " + RATING_MIN_REVIEWS + ") DESC, "
                    + "g.review_percent_positive DESC NULLS LAST, g.review_count DESC NULLS LAST, g.id";
            case NEWEST -> "g.release_date DESC NULLS LAST, g.id";
            case NAME -> "g.search_name, g.id";
        };
    }

    private static String ids(PlayerMode mode) {
        return String.join(", ", mode.featureIds().stream().map(String::valueOf).toList());
    }
}
