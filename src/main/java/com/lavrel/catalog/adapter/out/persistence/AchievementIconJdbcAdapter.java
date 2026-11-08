package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.application.port.out.AchievementIconPort;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** The trigger trg_achievement_icon (V31) normalizes every Steam address written to achievement.icon_url. */
@Component
public class AchievementIconJdbcAdapter implements AchievementIconPort {

    private static final String BASE = "(SELECT value FROM app_setting WHERE key = 'steam_image_base')";

    private final NamedParameterJdbcTemplate jdbc;

    public AchievementIconJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int useSteamImageBase(String base) {
        jdbc.update("""
                INSERT INTO app_setting (key, value) VALUES ('steam_image_base', :base)
                ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value
                """, new MapSqlParameterSource("base", base));
        return jdbc.update("""
                UPDATE achievement SET icon_url = %s || icon_path
                WHERE icon_path IS NOT NULL AND NOT icon_mirrored AND icon_url IS DISTINCT FROM %s || icon_path
                """.formatted(BASE, BASE), new MapSqlParameterSource());
    }

    @Override
    public boolean updateFromSteam(long gameId, String steamAchievementId, String steamIconUrl) {
        return jdbc.update("""
                UPDATE achievement SET icon_url = :url
                WHERE game_id = :gameId AND steam_achievement_id = :steamId
                """, new MapSqlParameterSource().addValue("url", steamIconUrl).addValue("gameId", gameId)
                .addValue("steamId", steamAchievementId)) > 0;
    }

    @Override
    public List<IconRef> findByGame(long gameId) {
        return jdbc.query("""
                SELECT id, icon_path, icon_mirrored FROM achievement
                WHERE game_id = :gameId AND icon_path IS NOT NULL ORDER BY id
                """, new MapSqlParameterSource("gameId", gameId), (rs, i) -> new IconRef(rs.getLong("id"),
                rs.getString("icon_path"), rs.getBoolean("icon_mirrored")));
    }

    @Override
    public void useMirror(long achievementId, String url) {
        jdbc.update("UPDATE achievement SET icon_url = :url, icon_mirrored = true WHERE id = :id",
                new MapSqlParameterSource().addValue("url", url).addValue("id", achievementId));
    }

    @Override
    public void useSteam(long achievementId) {
        jdbc.update("UPDATE achievement SET icon_mirrored = false, icon_url = " + BASE + " || icon_path "
                + "WHERE id = :id AND icon_path IS NOT NULL", new MapSqlParameterSource("id", achievementId));
    }
}
