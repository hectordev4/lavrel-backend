package com.lavrel.assistant.adapter.out.persistence;

import com.lavrel.assistant.application.port.out.GuideRepositoryPort;
import com.lavrel.assistant.domain.Guide;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** assistant_guide (migration V25); sources are stored as a small JSON array. */
@Component
public class GuideJdbcAdapter implements GuideRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public GuideJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Guide> find(long achievementId, String language) {
        return jdbc.query("""
                        SELECT achievement_id, language, text, grounded, sources, model, created_at, curated
                        FROM assistant_guide WHERE achievement_id = :id AND language = :language
                        """, new MapSqlParameterSource().addValue("id", achievementId).addValue("language", language),
                (rs, i) -> new Guide(rs.getLong("achievement_id"), rs.getString("language"), rs.getString("text"),
                        rs.getBoolean("grounded"), readSources(rs.getString("sources")), rs.getString("model"),
                        rs.getTimestamp("created_at").toInstant(), rs.getBoolean("curated"))).stream().findFirst();
    }

    @Override
    public void save(Guide g) {
        jdbc.update("""
                INSERT INTO assistant_guide (achievement_id, language, text, grounded, sources, model, created_at,
                                             curated)
                VALUES (:id, :language, :text, :grounded, :sources, :model, :createdAt, :curated)
                ON CONFLICT (achievement_id, language) DO UPDATE
                SET text = EXCLUDED.text, grounded = EXCLUDED.grounded, sources = EXCLUDED.sources,
                    model = EXCLUDED.model, created_at = EXCLUDED.created_at, curated = EXCLUDED.curated
                """, new MapSqlParameterSource()
                .addValue("id", g.achievementId())
                .addValue("language", g.language())
                .addValue("text", g.text())
                .addValue("grounded", g.grounded())
                .addValue("sources", mapper.writeValueAsString(g.sources()))
                .addValue("model", g.model())
                .addValue("createdAt", Timestamp.from(g.createdAt()))
                .addValue("curated", g.curated()));
    }

    @Override
    public int deleteByGame(long gameId) {
        return jdbc.update("""
                DELETE FROM assistant_guide
                WHERE achievement_id IN (SELECT id FROM achievement WHERE game_id = :gameId)
                """, new MapSqlParameterSource("gameId", gameId));
    }

    private List<Guide.Source> readSources(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return Arrays.asList(mapper.readValue(json, Guide.Source[].class));
    }
}
