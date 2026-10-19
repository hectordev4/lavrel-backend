package com.lavrel.platform.adapter.out.steam;

import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformAchievement;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformGameInfo;
import com.lavrel.library.application.port.out.PlatformLibraryPort;
import com.lavrel.library.application.port.out.PlatformLibraryPort.PlatformGame;
import com.lavrel.library.application.port.out.PlatformLibraryPort.PlatformUnlock;
import com.lavrel.library.domain.ProfilePrivateException;
import com.lavrel.platform.adapter.out.steam.SteamHttp.Response;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformUnavailableException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Steam implementation of both outbound ports: the user's library and unlocks (PlatformLibraryPort)
 * and the game and achievement definitions (PlatformCatalogPort). It only returns raw Steam data;
 * translation to our own IDs happens in the application services.
 */
@Component
public class SteamClient implements PlatformLibraryPort, PlatformCatalogPort {

    private static final String API = "https://api.steampowered.com";

    private final SteamHttp http;

    public SteamClient(SteamHttp http) {
        this.http = http;
    }

    @Override
    public Platform platform() {
        return Platform.STEAM;
    }

    // ---------------------------------------------------------------- library

    @Override
    public List<PlatformGame> fetchOwnedGames(String externalId) {
        Response response = http.get(API + "/IPlayerService/GetOwnedGames/v1/"
                        + "?key={key}&steamid={steamId}&include_appinfo=true&include_played_free_games=true&format=json",
                http.apiKey(), externalId);
        if (response.status() == 401 || response.status() == 403) {
            throw new PlatformUnavailableException(Platform.STEAM, "Steam rejected the API key");
        }
        requireOk(response);

        Map<String, Object> body = SteamJson.map(http.json(response).get("response"));
        // A private "game details" setting makes Steam answer with an empty object (no game_count at all).
        if (!body.containsKey("game_count")) {
            throw new ProfilePrivateException(externalId);
        }
        List<PlatformGame> games = new ArrayList<>();
        for (Object item : SteamJson.list(body.get("games"))) {
            Map<String, Object> game = SteamJson.map(item);
            long lastPlayed = SteamJson.longValue(game.get("rtime_last_played"));
            games.add(new PlatformGame(
                    String.valueOf(SteamJson.longValue(game.get("appid"))),
                    SteamJson.string(game.get("name")),
                    (int) SteamJson.longValue(game.get("playtime_forever")),
                    lastPlayed > 0 ? Instant.ofEpochSecond(lastPlayed) : null));
        }
        return games;
    }

    @Override
    public List<PlatformUnlock> fetchUnlocks(String externalId, String platformGameId) {
        Response response = http.get(API + "/ISteamUserStats/GetPlayerAchievements/v0001/"
                        + "?appid={appId}&key={key}&steamid={steamId}&l=english",
                platformGameId, http.apiKey(), externalId);
        if (response.status() == 403) {
            throw new ProfilePrivateException(externalId);   // "Profile is not public"
        }
        if (response.status() == 400) {
            return List.of();                                  // "Requested app has no stats"
        }
        requireOk(response);

        Map<String, Object> stats = SteamJson.map(http.json(response).get("playerstats"));
        List<PlatformUnlock> unlocks = new ArrayList<>();
        for (Object item : SteamJson.list(stats.get("achievements"))) {
            Map<String, Object> achievement = SteamJson.map(item);
            if (SteamJson.longValue(achievement.get("achieved")) != 1) {
                continue;
            }
            long unlockTime = SteamJson.longValue(achievement.get("unlocktime"));
            unlocks.add(new PlatformUnlock(
                    SteamJson.string(achievement.get("apiname")),
                    unlockTime > 0 ? Instant.ofEpochSecond(unlockTime) : Instant.EPOCH));
        }
        return unlocks;
    }

    // ---------------------------------------------------------------- catalog

    @Override
    public Optional<PlatformGameInfo> fetchGameInfo(String platformGameId) {
        Response response = http.get("https://store.steampowered.com/api/appdetails?appids={appId}&l=english",
                platformGameId);
        requireOk(response);

        Map<String, Object> entry = SteamJson.map(http.json(response).get(platformGameId));
        if (!Boolean.TRUE.equals(entry.get("success"))) {
            return Optional.empty();
        }
        Map<String, Object> data = SteamJson.map(entry.get("data"));
        String name = SteamJson.string(data.get("name"));
        if (name == null) {
            return Optional.empty();
        }
        return Optional.of(new PlatformGameInfo(platformGameId, name, SteamJson.string(data.get("header_image"))));
    }

    @Override
    public List<PlatformAchievement> fetchAchievements(String platformGameId) {
        Response response = http.get(API + "/ISteamUserStats/GetSchemaForGame/v2/?key={key}&appid={appId}&l=english",
                http.apiKey(), platformGameId);
        if (response.status() == 400) {
            return List.of();
        }
        requireOk(response);

        Map<String, Object> game = SteamJson.map(http.json(response).get("game"));
        Map<String, Object> gameStats = SteamJson.map(game.get("availableGameStats"));
        List<PlatformAchievement> achievements = new ArrayList<>();
        for (Object item : SteamJson.list(gameStats.get("achievements"))) {
            Map<String, Object> achievement = SteamJson.map(item);
            String apiName = SteamJson.string(achievement.get("name"));
            if (apiName == null) {
                continue;
            }
            String displayName = SteamJson.string(achievement.get("displayName"));
            achievements.add(new PlatformAchievement(
                    apiName,
                    displayName != null ? displayName : apiName,
                    SteamJson.string(achievement.get("description")),
                    SteamJson.string(achievement.get("icon")),
                    SteamJson.longValue(achievement.get("hidden")) == 1));
        }
        return achievements;
    }

    private static void requireOk(Response response) {
        if (!response.ok()) {
            throw new PlatformUnavailableException(Platform.STEAM, "unexpected HTTP status " + response.status());
        }
    }
}
