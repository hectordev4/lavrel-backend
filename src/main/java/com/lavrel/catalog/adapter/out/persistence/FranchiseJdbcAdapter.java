package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.application.port.out.FranchiseBlocklistPort;
import com.lavrel.catalog.application.port.out.FranchiseLinksPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

/** franchise_blocklist and the FRANCHISE rows of game_creator. */
@Component
public class FranchiseJdbcAdapter implements FranchiseBlocklistPort, FranchiseLinksPort {

    private final NamedParameterJdbcTemplate jdbc;

    public FranchiseJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Set<String> keys() {
        return new HashSet<>(jdbc.queryForList("SELECT name_key FROM franchise_blocklist",
                new MapSqlParameterSource(), String.class));
    }

    @Override
    public List<BlockedFranchise> list() {
        return jdbc.query("SELECT name_key, name, added_at FROM franchise_blocklist ORDER BY name",
                new MapSqlParameterSource(), (rs, i) -> new BlockedFranchise(rs.getString("name_key"),
                        rs.getString("name"), rs.getTimestamp("added_at").toInstant()));
    }

    @Override
    public void add(String key, String name, Instant at) {
        jdbc.update("""
                INSERT INTO franchise_blocklist (name_key, name, added_at) VALUES (:key, :name, :at)
                ON CONFLICT (name_key) DO NOTHING
                """, new MapSqlParameterSource().addValue("key", key).addValue("name", name)
                .addValue("at", Timestamp.from(at)));
    }

    @Override
    public boolean remove(String key) {
        return jdbc.update("DELETE FROM franchise_blocklist WHERE name_key = :key",
                new MapSqlParameterSource("key", key)) > 0;
    }

    @Override
    public List<FranchiseLink> findAll() {
        return jdbc.query("""
                SELECT gc.game_id, c.id AS creator_id, c.name,
                       (SELECT string_agg(c2.name, '|') FROM game_creator g2 JOIN creator c2 ON c2.id = g2.creator_id
                        WHERE g2.game_id = gc.game_id AND g2.role = 'DEVELOPER') AS developers,
                       (SELECT string_agg(c3.name, '|') FROM game_creator g3 JOIN creator c3 ON c3.id = g3.creator_id
                        WHERE g3.game_id = gc.game_id AND g3.role = 'PUBLISHER') AS publishers
                FROM game_creator gc
                JOIN creator c ON c.id = gc.creator_id
                WHERE gc.role = 'FRANCHISE'
                """, new MapSqlParameterSource(), (rs, i) -> new FranchiseLink(rs.getLong("game_id"),
                rs.getLong("creator_id"), rs.getString("name"), split(rs.getString("developers")),
                split(rs.getString("publishers"))));
    }

    @Override
    public int unlink(List<FranchiseLink> links) {
        if (links.isEmpty()) {
            return 0;
        }
        int[] counts = jdbc.batchUpdate("""
                DELETE FROM game_creator WHERE game_id = :gameId AND creator_id = :creatorId AND role = 'FRANCHISE'
                """, links.stream().map(l -> new MapSqlParameterSource().addValue("gameId", l.gameId())
                .addValue("creatorId", l.creatorId())).toArray(SqlParameterSource[]::new));
        return Arrays.stream(counts).map(c -> Math.max(c, 0)).sum();
    }

    @Override
    public int unlinkCreator(long creatorId) {
        return jdbc.update("DELETE FROM game_creator WHERE creator_id = :id AND role = 'FRANCHISE'",
                new MapSqlParameterSource("id", creatorId));
    }

    @Override
    public Optional<String> creatorName(long creatorId) {
        return jdbc.queryForList("SELECT name FROM creator WHERE id = :id", new MapSqlParameterSource("id", creatorId),
                String.class).stream().findFirst();
    }

    private static List<String> split(String names) {
        return names == null ? List.of() : Arrays.asList(names.split("\\|"));
    }
}
