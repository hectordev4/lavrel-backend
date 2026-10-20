package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.application.port.out.ImportFailureRepositoryPort;
import com.lavrel.shared.domain.Platform;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** Plain SQL: this small bookkeeping table needs no JPA entity. */
@Component
public class ImportFailureJdbcAdapter implements ImportFailureRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public ImportFailureJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Set<String> findFailedSince(Platform platform, Collection<String> platformGameIds, Instant since,
                                       Collection<String> ignoredReasons) {
        if (platformGameIds.isEmpty()) {
            return Set.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("platform", platform.name())
                .addValue("ids", platformGameIds)
                .addValue("since", Timestamp.from(since));
        String sql = """
                SELECT platform_game_id
                FROM catalog_import_failure
                WHERE platform = :platform
                  AND platform_game_id IN (:ids)
                  AND failed_at >= :since
                """;
        if (!ignoredReasons.isEmpty()) {
            sql += " AND reason NOT IN (:ignoredReasons)";
            params.addValue("ignoredReasons", ignoredReasons);
        }
        return new HashSet<>(jdbc.queryForList(sql, params, String.class));
    }

    @Override
    public void recordFailure(Platform platform, String platformGameId, String reason, Instant at) {
        jdbc.update("""
                INSERT INTO catalog_import_failure (platform, platform_game_id, reason, attempts, failed_at)
                VALUES (:platform, :id, :reason, 1, :at)
                ON CONFLICT (platform, platform_game_id) DO UPDATE
                SET reason = EXCLUDED.reason,
                    attempts = catalog_import_failure.attempts + 1,
                    failed_at = EXCLUDED.failed_at
                """, new MapSqlParameterSource()
                .addValue("platform", platform.name())
                .addValue("id", platformGameId)
                .addValue("reason", reason.length() > 255 ? reason.substring(0, 255) : reason)
                .addValue("at", Timestamp.from(at)));
    }
}
