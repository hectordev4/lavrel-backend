package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.CreatorOption;
import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.CreatorRole;
import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.TagOption;
import com.lavrel.catalog.application.port.out.FilterOptionsQueryPort;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tag and creator options with their number of published games. Names are matched with lavrel_normalize like game
 * names (migration V16); with a query, names starting with it come first, then the most used.
 */
@Component
public class FilterOptionsJdbcAdapter implements FilterOptionsQueryPort {

    private static final String TAGS = """
            SELECT t.id, t.name, COUNT(*) AS games
            FROM tag t
            JOIN game_tag gt ON gt.tag_id = t.id AND gt.position < 10
            JOIN game g ON g.id = gt.game_id AND g.enriched_at IS NOT NULL
            WHERE CAST(:q AS text) IS NULL OR lavrel_normalize(t.name) LIKE '%' || lavrel_normalize(:q) || '%'
            GROUP BY t.id, t.name
            ORDER BY CASE WHEN CAST(:q AS text) IS NOT NULL
                           AND lavrel_normalize(t.name) LIKE lavrel_normalize(:q) || '%' THEN 0 ELSE 1 END,
                     games DESC, t.name
            LIMIT :limit
            """;

    private static final String CREATORS = """
            SELECT c.id, c.name, COUNT(DISTINCT gc.game_id) AS games
            FROM creator c
            JOIN game_creator gc ON gc.creator_id = c.id AND gc.role = :role
            JOIN game g ON g.id = gc.game_id AND g.enriched_at IS NOT NULL
            WHERE CAST(:q AS text) IS NULL OR lavrel_normalize(c.name) LIKE '%' || lavrel_normalize(:q) || '%'
            GROUP BY c.id, c.name
            ORDER BY CASE WHEN CAST(:q AS text) IS NOT NULL
                           AND lavrel_normalize(c.name) LIKE lavrel_normalize(:q) || '%' THEN 0 ELSE 1 END,
                     games DESC, c.name
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public FilterOptionsJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagOption> tags(String query, int limit) {
        return jdbc.query(TAGS, new MapSqlParameterSource().addValue("q", query).addValue("limit", limit),
                (rs, i) -> new TagOption(rs.getLong("id"), rs.getString("name"), rs.getInt("games")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreatorOption> creators(CreatorRole role, String query, int limit) {
        return jdbc.query(CREATORS, new MapSqlParameterSource()
                        .addValue("role", role.name()).addValue("q", query).addValue("limit", limit),
                (rs, i) -> new CreatorOption(rs.getLong("id"), rs.getString("name"), role, rs.getInt("games")));
    }
}
