package com.lavrel.profile.adapter.out.persistence;

import com.lavrel.profile.application.port.out.ProfileSettingsRepositoryPort;
import com.lavrel.profile.domain.ProfileSettings;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProfileSettingsJdbcAdapter implements ProfileSettingsRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public ProfileSettingsJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ProfileSettings> find(long userId) {
        return jdbc.query("""
                        SELECT user_id, profile_public, show_country, show_platforms, show_avatar
                        FROM profile_settings WHERE user_id = :userId
                        """, new MapSqlParameterSource("userId", userId),
                (rs, i) -> ProfileSettings.restore(rs.getLong("user_id"), rs.getBoolean("profile_public"),
                        rs.getBoolean("show_country"), rs.getBoolean("show_platforms"), rs.getBoolean("show_avatar")))
                .stream().findFirst();
    }

    @Override
    public void save(ProfileSettings s) {
        jdbc.update("""
                INSERT INTO profile_settings (user_id, profile_public, show_country, show_platforms, show_avatar)
                VALUES (:userId, :public, :country, :platforms, :avatar)
                ON CONFLICT (user_id) DO UPDATE
                SET profile_public = EXCLUDED.profile_public, show_country = EXCLUDED.show_country,
                    show_platforms = EXCLUDED.show_platforms, show_avatar = EXCLUDED.show_avatar
                """, new MapSqlParameterSource()
                .addValue("userId", s.userId())
                .addValue("public", s.profilePublic())
                .addValue("country", s.showCountry())
                .addValue("platforms", s.showPlatforms())
                .addValue("avatar", s.showAvatar()));
    }
}
