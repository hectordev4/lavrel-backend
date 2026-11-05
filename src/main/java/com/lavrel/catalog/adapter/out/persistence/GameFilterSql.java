package com.lavrel.catalog.adapter.out.persistence;

/**
 * SQL fragments shared by every query that filters games the way search does (catalog search, the user's
 * library). The game table must be aliased {@code g}; the name filter needs {@link #QUERY_JOIN} and a {@code :q}
 * parameter. Keeping them here means "what a tag means for a game" is decided in one place.
 */
public final class GameFilterSql {

    /** A tag describes a game when it is among its top 10, or holds at least a quarter of its top tag's weight. */
    public static final int TAG_TOP_POSITIONS = 10;

    /** Normalizes the text query once (lavrel_normalize, migration V16); use as {@code q.v}. */
    public static final String QUERY_JOIN = "CROSS JOIN (SELECT lavrel_normalize(:q) AS v) q\n";

    /** Typo-tolerant name match: contains the query, or close to a part of the name (pg_trgm). */
    public static final String NAME_MATCH = "(g.search_name LIKE '%' || q.v || '%' OR q.v <% g.search_name)";

    private GameFilterSql() {
    }

    /** The game has the tag bound to {@code :param}, and the tag really describes it. */
    public static String tagCondition(String param) {
        return """
                EXISTS (SELECT 1 FROM game_tag gt WHERE gt.game_id = g.id AND gt.tag_id = :%s
                        AND (gt.position < %d
                             OR gt.weight * 4 >= (SELECT max(t0.weight) FROM game_tag t0 WHERE t0.game_id = g.id)))"""
                .formatted(param, TAG_TOP_POSITIONS);
    }

    /** The systems the game is available on, as "PC|XBOX" (select column). */
    public static final String SYSTEMS_COLUMN =
            "(SELECT string_agg(gs.system, '|' ORDER BY gs.system) FROM game_system gs WHERE gs.game_id = g.id)";

    /** The game is available on the system bound to {@code :param} (its name, e.g. "XBOX"). */
    public static String systemCondition(String param) {
        return "EXISTS (SELECT 1 FROM game_system gs WHERE gs.game_id = g.id AND gs.system = :" + param + ")";
    }

    /** The game offers one of the Steam categories bound to {@code :param} (a player mode). */
    public static String modeCondition(String param) {
        return "EXISTS (SELECT 1 FROM game_feature gf WHERE gf.game_id = g.id AND gf.feature_id IN (:" + param + "))";
    }
}
