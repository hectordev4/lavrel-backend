package com.lavrel.profile.adapter.out.persistence;

import com.lavrel.profile.application.port.in.ViewProfilesUseCase.Country;
import com.lavrel.profile.application.port.in.ViewProfilesUseCase.ProfileHeader;
import com.lavrel.profile.application.port.out.PublicProfileQueryPort;
import com.lavrel.shared.domain.Platform;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Every query joins profile_settings with profile_public = true, and the opt-in columns decide in SQL whether country
 * and platform badges are selected at all: what the user keeps private never leaves the database.
 */
@Component
public class PublicProfileJdbcAdapter implements PublicProfileQueryPort {

    private static final String HEADER = """
            SELECT u.public_id, u.username, u.created_at,
                   CASE WHEN ps.show_country THEN c.code END AS country_code,
                   CASE WHEN ps.show_country THEN c.name END AS country_name,
                   CASE WHEN ps.show_platforms
                        THEN (SELECT string_agg(DISTINCT pa.platform, '|') FROM platform_account pa
                              WHERE pa.user_id = u.id) END AS platforms
            FROM users u
            JOIN profile_settings ps ON ps.user_id = u.id AND ps.profile_public
            LEFT JOIN country c ON c.id = u.country_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public PublicProfileJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ProfileHeader> findHeader(long publicId) {
        return jdbc.query(HEADER + "WHERE u.public_id = :publicId", new MapSqlParameterSource("publicId", publicId),
                PublicProfileJdbcAdapter::header).stream().findFirst();
    }

    @Override
    public Optional<Long> findUserId(long publicId) {
        return jdbc.queryForList("""
                        SELECT u.id FROM users u JOIN profile_settings ps ON ps.user_id = u.id AND ps.profile_public
                        WHERE u.public_id = :publicId
                        """, new MapSqlParameterSource("publicId", publicId), Long.class)
                .stream().findFirst();
    }

    @Override
    public List<ProfileHeader> search(String query, Long exactPublicId, int limit, long offset) {
        return jdbc.query(HEADER + """
                        WHERE (CAST(:exactId AS bigint) IS NOT NULL AND u.public_id = :exactId)
                           OR (CAST(:exactId AS bigint) IS NULL
                               AND lavrel_normalize(u.username) LIKE '%' || lavrel_normalize(:q) || '%')
                        ORDER BY (lavrel_normalize(u.username) = lavrel_normalize(:q)) DESC,
                                 lavrel_normalize(u.username), u.public_id
                        LIMIT :limit OFFSET :offset
                        """, new MapSqlParameterSource()
                        .addValue("q", query)
                        .addValue("exactId", exactPublicId, Types.BIGINT)
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                PublicProfileJdbcAdapter::header);
    }

    private static ProfileHeader header(ResultSet rs, int i) throws SQLException {
        String code = rs.getString("country_code");
        String platforms = rs.getString("platforms");
        return new ProfileHeader(rs.getLong("public_id"), rs.getString("username"),
                YearMonth.from(rs.getTimestamp("created_at").toInstant().atZone(ZoneOffset.UTC)),
                code == null ? null : new Country(code, rs.getString("country_name")),
                platforms == null ? List.of() : Arrays.stream(platforms.split("\\|")).map(Platform::valueOf).toList());
    }
}
