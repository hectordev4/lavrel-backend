package com.lavrel.library.adapter.out.persistence;

import com.lavrel.library.application.port.out.DueAccountsQueryPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DueAccountsJdbcAdapter implements DueAccountsQueryPort {

    private static final String SQL = """
            SELECT pa.id
            FROM platform_account pa
            JOIN users u ON u.id = pa.user_id
            WHERE u.auto_sync
              AND u.last_seen_at >= :seenAfter
              AND (pa.last_synced_at IS NULL OR pa.last_synced_at < :syncedBefore)
            ORDER BY pa.last_synced_at NULLS FIRST, pa.id
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public DueAccountsJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Long> findDue(Instant syncedBefore, Instant seenAfter, int limit) {
        return jdbc.queryForList(SQL, new MapSqlParameterSource()
                .addValue("syncedBefore", Timestamp.from(syncedBefore))
                .addValue("seenAfter", Timestamp.from(seenAfter))
                .addValue("limit", limit), Long.class);
    }
}
