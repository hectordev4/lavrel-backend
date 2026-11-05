package com.lavrel.user.adapter.out.persistence;

import com.lavrel.user.application.port.out.UserActivityPort;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * last_seen_at only needs hour precision (the automatic sync looks at the last 30 days), so the last write per user is
 * remembered in memory and the database is touched at most once an hour per user.
 */
@Component
public class UserActivityJdbcAdapter implements UserActivityPort {

    private static final Duration PRECISION = Duration.ofHours(1);

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<Long, Instant> lastWritten = new ConcurrentHashMap<>();

    public UserActivityJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void recordSeen(long userId, Instant at) {
        Instant previous = lastWritten.get(userId);
        if (previous != null && previous.plus(PRECISION).isAfter(at)) {
            return;
        }
        jdbc.update("UPDATE users SET last_seen_at = :at WHERE id = :id",
                new MapSqlParameterSource().addValue("at", Timestamp.from(at)).addValue("id", userId));
        lastWritten.put(userId, at);
    }

    @Override
    public boolean autoSyncEnabled(long userId) {
        Boolean enabled = jdbc.queryForObject("SELECT auto_sync FROM users WHERE id = :id",
                new MapSqlParameterSource("id", userId), Boolean.class);
        return Boolean.TRUE.equals(enabled);
    }

    @Override
    public void setAutoSync(long userId, boolean enabled) {
        jdbc.update("UPDATE users SET auto_sync = :enabled WHERE id = :id",
                new MapSqlParameterSource().addValue("enabled", enabled).addValue("id", userId));
    }
}
