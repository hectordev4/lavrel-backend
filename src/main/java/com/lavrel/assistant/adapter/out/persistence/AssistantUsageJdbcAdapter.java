package com.lavrel.assistant.adapter.out.persistence;

import com.lavrel.assistant.application.port.out.AssistantUsagePort;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One atomic statement per check: it adds one only while the counter is below the limit, so two requests at the same
 * moment can never both slip past it.
 */
@Component
public class AssistantUsageJdbcAdapter implements AssistantUsagePort {

    private final NamedParameterJdbcTemplate jdbc;

    public AssistantUsageJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean tryConsume(String scope, LocalDate day, int limit) {
        if (limit <= 0) {
            return false;
        }
        List<Integer> updated = jdbc.queryForList("""
                INSERT INTO assistant_usage (scope, day, count) VALUES (:scope, :day, 1)
                ON CONFLICT (scope, day) DO UPDATE SET count = assistant_usage.count + 1
                WHERE assistant_usage.count < :limit
                RETURNING count
                """, new MapSqlParameterSource().addValue("scope", scope).addValue("day", Date.valueOf(day))
                .addValue("limit", limit), Integer.class);
        return !updated.isEmpty();
    }

    @Override
    public int used(String scope, LocalDate day) {
        List<Integer> count = jdbc.queryForList(
                "SELECT count FROM assistant_usage WHERE scope = :scope AND day = :day",
                new MapSqlParameterSource().addValue("scope", scope).addValue("day", Date.valueOf(day)),
                Integer.class);
        return count.isEmpty() ? 0 : count.get(0);
    }
}
