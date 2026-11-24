package com.lavrel.assistant.adapter.out.persistence;

import com.lavrel.assistant.application.port.out.ReferenceWikiRepositoryPort;
import com.lavrel.assistant.domain.ReferenceWiki;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReferenceWikiJdbcAdapter implements ReferenceWikiRepositoryPort {

    private static final String COLUMNS = "w.id, w.api_url, w.site_name, w.license, w.license_url, w.article_url, "
            + "w.added_at";

    private final NamedParameterJdbcTemplate jdbc;

    public ReferenceWikiJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ReferenceWiki> findForGame(long gameId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM game_wiki g JOIN reference_wiki w ON w.id = g.wiki_id "
                        + "WHERE g.game_id = :gameId",
                new MapSqlParameterSource("gameId", gameId), ReferenceWikiJdbcAdapter::map).stream().findFirst();
    }

    @Override
    public Optional<ReferenceWiki> findByApiUrl(String apiUrl) {
        return jdbc.query("SELECT " + COLUMNS + " FROM reference_wiki w WHERE w.api_url = :apiUrl",
                new MapSqlParameterSource("apiUrl", apiUrl), ReferenceWikiJdbcAdapter::map).stream().findFirst();
    }

    @Override
    public ReferenceWiki save(ReferenceWiki w) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", w.id())
                .addValue("apiUrl", w.apiUrl())
                .addValue("siteName", w.siteName())
                .addValue("license", w.license())
                .addValue("licenseUrl", w.licenseUrl())
                .addValue("articleUrl", w.articleUrl())
                .addValue("addedAt", Timestamp.from(w.addedAt()));
        if (w.id() != null) {
            jdbc.update("""
                    UPDATE reference_wiki
                    SET site_name = :siteName, license = :license, license_url = :licenseUrl, article_url = :articleUrl
                    WHERE id = :id
                    """, params);
            return w;
        }
        // Two admins approving the same new wiki at once: the unique api_url keeps one row, both get it.
        Long id = jdbc.queryForObject("""
                INSERT INTO reference_wiki (api_url, site_name, license, license_url, article_url, added_at)
                VALUES (:apiUrl, :siteName, :license, :licenseUrl, :articleUrl, :addedAt)
                ON CONFLICT (api_url) DO UPDATE
                SET site_name = EXCLUDED.site_name, license = EXCLUDED.license,
                    license_url = EXCLUDED.license_url, article_url = EXCLUDED.article_url
                RETURNING id
                """, params, Long.class);
        return w.withId(id);
    }

    @Override
    public void link(long gameId, long wikiId, Instant at) {
        jdbc.update("""
                INSERT INTO game_wiki (game_id, wiki_id, added_at) VALUES (:gameId, :wikiId, :at)
                ON CONFLICT (game_id) DO UPDATE SET wiki_id = EXCLUDED.wiki_id, added_at = EXCLUDED.added_at
                """, new MapSqlParameterSource().addValue("gameId", gameId).addValue("wikiId", wikiId)
                .addValue("at", Timestamp.from(at)));
    }

    @Override
    public boolean unlink(long gameId) {
        return jdbc.update("DELETE FROM game_wiki WHERE game_id = :gameId",
                new MapSqlParameterSource("gameId", gameId)) > 0;
    }

    @Override
    public int deleteUnused() {
        return jdbc.update("DELETE FROM reference_wiki w WHERE NOT EXISTS (SELECT 1 FROM game_wiki g WHERE g.wiki_id = w.id)",
                new MapSqlParameterSource());
    }

    @Override
    public List<WikiWithGames> findAllWithGames() {
        Map<Long, ReferenceWiki> wikis = new LinkedHashMap<>();
        Map<Long, List<Long>> games = new LinkedHashMap<>();
        jdbc.query("SELECT " + COLUMNS + ", g.game_id FROM reference_wiki w "
                        + "LEFT JOIN game_wiki g ON g.wiki_id = w.id "
                        + "ORDER BY coalesce(w.site_name, w.api_url), w.id, g.game_id",
                new MapSqlParameterSource(), rs -> {
                    long id = rs.getLong("id");
                    wikis.computeIfAbsent(id, k -> mapUnchecked(rs));
                    List<Long> ids = games.computeIfAbsent(id, k -> new ArrayList<>());
                    long gameId = rs.getLong("game_id");
                    if (!rs.wasNull()) {
                        ids.add(gameId);
                    }
                });
        List<WikiWithGames> result = new ArrayList<>();
        wikis.forEach((id, wiki) -> result.add(new WikiWithGames(wiki, List.copyOf(games.get(id)))));
        return result;
    }

    private static ReferenceWiki mapUnchecked(ResultSet rs) {
        try {
            return map(rs, 0);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ReferenceWiki map(ResultSet rs, int i) throws SQLException {
        return new ReferenceWiki(rs.getLong("id"), rs.getString("api_url"), rs.getString("site_name"),
                rs.getString("license"), rs.getString("license_url"), rs.getString("article_url"),
                rs.getTimestamp("added_at").toInstant());
    }
}
