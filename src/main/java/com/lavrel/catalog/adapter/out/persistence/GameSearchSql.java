package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GameSearchCriteria;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.PlayerMode;
import com.lavrel.catalog.domain.GameSystem;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Builds the search SQL from the criteria. Kept apart from the adapter so the query shape can be unit tested.
 * Every user value goes in as a bind parameter; only fixed fragments are concatenated.
 *
 * Name matching uses lavrel_normalize (lower case, no accents, punctuation as spaces) on both sides and pg_trgm,
 * so "witcer 3" finds "The Witcher® 3: Wild Hunt". The name, tag and mode conditions come from GameFilterSql, which the
 * library query shares.
 */
final class GameSearchSql {

    static final int RATING_MIN_REVIEWS = 100;

    private static final String SELECT = """
            SELECT g.id, g.title, g.cover_image_url, g.release_date, g.windows, g.mac, g.linux,
                   g.review_count, g.review_percent_positive,
                   (SELECT string_agg(t.name, '|' ORDER BY gt.position)
                    FROM game_tag gt JOIN tag t ON t.id = gt.tag_id
                    WHERE gt.game_id = g.id AND gt.position < 3) AS top_tags,
                   EXISTS (SELECT 1 FROM game_feature gf WHERE gf.game_id = g.id AND gf.feature_id IN (%s)) AS single_player,
                   EXISTS (SELECT 1 FROM game_feature gf WHERE gf.game_id = g.id AND gf.feature_id IN (%s)) AS co_op,
                   EXISTS (SELECT 1 FROM game_feature gf WHERE gf.game_id = g.id AND gf.feature_id IN (%s)) AS multiplayer,
                   %s AS systems
            FROM game g
            """.formatted(ids(PlayerMode.SINGLE_PLAYER), ids(PlayerMode.CO_OP), ids(PlayerMode.MULTIPLAYER),
                    GameFilterSql.SYSTEMS_COLUMN);

    private GameSearchSql() {
    }

    record Query(String sql, MapSqlParameterSource params) {
    }

    /** Fetches one row more than the page size, so the adapter can tell whether there is a next page. */
    static Query build(GameSearchCriteria c) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder(SELECT);
        if (c.query() != null) {
            sql.append(GameFilterSql.QUERY_JOIN);
            params.addValue("q", c.query());
        }
        List<String> where = new ArrayList<>();
        where.add("g.enriched_at IS NOT NULL");
        if (c.query() != null) {
            where.add(GameFilterSql.NAME_MATCH);
        }
        for (int i = 0; i < c.tagIds().size(); i++) {
            String p = "tag" + i;
            where.add(GameFilterSql.tagCondition(p));
            params.addValue(p, c.tagIds().get(i));
        }
        creator(where, params, "franchiseId", c.franchiseId(), "FRANCHISE");
        creator(where, params, "developerId", c.developerId(), "DEVELOPER");
        creator(where, params, "publisherId", c.publisherId(), "PUBLISHER");
        int m = 0;
        for (PlayerMode mode : c.modes().stream().sorted().toList()) {
            String p = "mode" + m++;
            where.add(GameFilterSql.modeCondition(p));
            params.addValue(p, mode.featureIds());
        }
        int s = 0;
        for (GameSystem system : c.systems().stream().sorted().toList()) {
            String p = "system" + s++;
            where.add(GameFilterSql.systemCondition(p));
            params.addValue(p, system.name());
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
