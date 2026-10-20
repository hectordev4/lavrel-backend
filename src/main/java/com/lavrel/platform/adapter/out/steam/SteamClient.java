package com.lavrel.platform.adapter.out.steam;

import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.GameIdPage;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformAchievement;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformGameInfo;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.StoreItem;
import com.lavrel.catalog.domain.GameMetadata.AgeRating;
import com.lavrel.catalog.domain.GameMetadata.Creator;
import com.lavrel.catalog.domain.GameMetadata.TagWeight;
import com.lavrel.library.application.port.out.PlatformLibraryPort;
import com.lavrel.library.application.port.out.PlatformLibraryPort.PlatformGame;
import com.lavrel.library.application.port.out.PlatformLibraryPort.PlatformUnlock;
import com.lavrel.library.domain.ProfilePrivateException;
import com.lavrel.platform.adapter.out.steam.SteamHttp.Response;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformUnavailableException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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

    /** The full store page of one game (appdetails). */
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

        List<Integer> contentDescriptorIds = new ArrayList<>();
        for (Object id : SteamJson.list(SteamJson.map(data.get("content_descriptors")).get("ids"))) {
            contentDescriptorIds.add((int) SteamJson.longValue(id));
        }
        Map<Integer, String> genres = new LinkedHashMap<>();
        for (Object genre : SteamJson.list(data.get("genres"))) {
            Map<String, Object> g = SteamJson.map(genre);
            String description = SteamJson.string(g.get("description"));
            long genreId = SteamJson.longValue(g.get("id"));
            if (description != null && genreId > 0) {
                genres.put((int) genreId, description);
            }
        }
        Map<Integer, String> categories = new LinkedHashMap<>();
        for (Object category : SteamJson.list(data.get("categories"))) {
            Map<String, Object> c = SteamJson.map(category);
            String description = SteamJson.string(c.get("description"));
            if (description != null) {
                categories.put((int) SteamJson.longValue(c.get("id")), description);
            }
        }
        Map<String, Object> platforms = SteamJson.map(data.get("platforms"));
        long requiredAge = SteamJson.longValue(data.get("required_age"));

        return Optional.of(new PlatformGameInfo(
                platformGameId,
                name,
                SteamJson.string(data.get("header_image")),
                SteamJson.string(data.get("short_description")),
                SteamJson.string(data.get("type")),
                contentDescriptorIds,
                genres,
                categories,
                ageRatings(SteamJson.map(data.get("ratings"))),
                Boolean.TRUE.equals(data.get("is_free")),
                requiredAge > 0 ? (int) requiredAge : null,
                Boolean.TRUE.equals(platforms.get("windows")),
                Boolean.TRUE.equals(platforms.get("mac")),
                Boolean.TRUE.equals(platforms.get("linux")),
                strings(data.get("developers")),
                strings(data.get("publishers"))));
    }

    /** Age ratings per board (PEGI, ESRB, USK...). The "steam_*" entries are Steam's own duplicates: skipped. */
    private static List<AgeRating> ageRatings(Map<String, Object> ratings) {
        List<AgeRating> result = new ArrayList<>();
        ratings.forEach((board, value) -> {
            if (board.startsWith("steam_")) {
                return;
            }
            Map<String, Object> rating = SteamJson.map(value);
            String label = SteamJson.string(rating.get("rating"));
            if (label == null || label.isBlank()) {
                return;
            }
            String descriptors = SteamJson.string(rating.get("descriptors"));
            List<String> lines = descriptors == null ? List.of()
                    : Arrays.stream(descriptors.split("\\r?\\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            result.add(new AgeRating(board, label.trim(), lines));
        });
        return result;
    }

    private static List<String> strings(Object value) {
        return SteamJson.list(value).stream().map(SteamJson::string).filter(s -> s != null && !s.isBlank()).toList();
    }

    /**
     * IStoreBrowseService/GetItems: lightweight data for many games in ONE call. It is the store's own backend
     * (public, but not part of Valve's documented API, so it could change).
     */
    @Override
    public Map<String, StoreItem> fetchStoreItems(List<String> platformGameIds) {
        List<String> ids = platformGameIds.stream().filter(id -> id.matches("\\d{1,10}")).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        String input = "{\"ids\":[" + ids.stream().map(id -> "{\"appid\":" + id + "}").collect(Collectors.joining(","))
                + "],\"context\":{\"language\":\"english\",\"country_code\":\"US\"},"
                + "\"data_request\":{\"include_tag_count\":20,\"include_basic_info\":true,\"include_release\":true,"
                + "\"include_platforms\":true,\"include_ratings\":true,\"include_reviews\":true}}";
        Response response = http.get(API + "/IStoreBrowseService/GetItems/v1?key={key}&input_json={input}",
                http.apiKey(), input);
        requireOk(response);

        Map<String, StoreItem> items = new LinkedHashMap<>();
        for (Object raw : SteamJson.list(SteamJson.map(http.json(response).get("response")).get("store_items"))) {
            Map<String, Object> item = SteamJson.map(raw);
            long appId = SteamJson.longValue(item.get("appid") != null ? item.get("appid") : item.get("id"));
            if (appId > 0) {
                items.put(String.valueOf(appId), storeItem(String.valueOf(appId), item));
            }
        }
        return items;
    }

    private static StoreItem storeItem(String appId, Map<String, Object> item) {
        boolean found = SteamJson.longValue(item.get("success")) == 1 && !Boolean.FALSE.equals(item.get("visible"));

        List<TagWeight> tags = new ArrayList<>();
        for (Object tag : SteamJson.list(item.get("tags"))) {
            Map<String, Object> t = SteamJson.map(tag);
            tags.add(new TagWeight(SteamJson.longValue(t.get("tagid")), null, (int) SteamJson.longValue(t.get("weight"))));
        }
        Map<String, Object> categories = SteamJson.map(item.get("categories"));
        Map<String, Object> review = SteamJson.map(SteamJson.map(item.get("reviews")).get("summary_filtered"));
        Map<String, Object> basic = SteamJson.map(item.get("basic_info"));
        Map<String, Object> platforms = SteamJson.map(item.get("platforms"));
        Map<String, Object> rating = SteamJson.map(item.get("game_rating"));
        long releaseEpoch = SteamJson.longValue(SteamJson.map(item.get("release")).get("steam_release_date"));
        long requiredAge = SteamJson.longValue(rating.get("required_age"));

        return new StoreItem(
                appId, found, storeAppType((int) SteamJson.longValue(item.get("type"))),
                SteamJson.string(item.get("name")), SteamJson.string(basic.get("short_description")),
                tags,
                ints(item.get("content_descriptorids")),
                ints(categories.get("supported_player_categoryids")),
                ints(categories.get("feature_categoryids")),
                ints(categories.get("controller_categoryids")),
                review.containsKey("review_count") ? (int) SteamJson.longValue(review.get("review_count")) : null,
                review.containsKey("percent_positive") ? (int) SteamJson.longValue(review.get("percent_positive")) : null,
                releaseEpoch > 0 ? releaseEpoch : null,
                Boolean.TRUE.equals(platforms.get("windows")),
                Boolean.TRUE.equals(platforms.get("mac")),
                Boolean.TRUE.equals(platforms.get("linux")),
                SteamJson.string(rating.get("rating")),
                requiredAge > 0 ? (int) requiredAge : null,
                creators(basic.get("developers")), creators(basic.get("publishers")), creators(basic.get("franchises")));
    }

    private static List<Creator> creators(Object value) {
        List<Creator> creators = new ArrayList<>();
        for (Object raw : SteamJson.list(value)) {
            Map<String, Object> creator = SteamJson.map(raw);
            String name = SteamJson.string(creator.get("name"));
            if (name != null && !name.isBlank()) {
                long steamId = SteamJson.longValue(creator.get("creator_clan_account_id"));
                creators.add(new Creator(name.trim(), steamId > 0 ? steamId : null));
            }
        }
        return creators;
    }

    private static List<Integer> ints(Object value) {
        return SteamJson.list(value).stream().map(v -> (int) SteamJson.longValue(v)).toList();
    }

    /** Steam's EStoreAppType, as the lowercase names the store page uses. */
    private static String storeAppType(int type) {
        return switch (type) {
            case 0 -> "game";
            case 1 -> "demo";
            case 2 -> "mod";
            case 3 -> "movie";
            case 4 -> "dlc";
            case 5 -> "guide";
            case 6 -> "software";
            case 7 -> "video";
            case 8 -> "series";
            case 9 -> "episode";
            case 10 -> "hardware";
            case 11 -> "music";
            case 12 -> "beta";
            case 13 -> "tool";
            case 14 -> "advertising";
            default -> "unknown";
        };
    }

    /** IStoreService/GetTagList: every tag with its name (about 400). */
    @Override
    public Map<Long, String> fetchTagNames() {
        Response response = http.get(API + "/IStoreService/GetTagList/v1/?key={key}&language=english", http.apiKey());
        requireOk(response);

        Map<Long, String> names = new LinkedHashMap<>();
        for (Object raw : SteamJson.list(SteamJson.map(http.json(response).get("response")).get("tags"))) {
            Map<String, Object> tag = SteamJson.map(raw);
            String name = SteamJson.string(tag.get("name"));
            long id = SteamJson.longValue(tag.get("tagid"));
            if (id > 0 && name != null && !name.isBlank()) {
                names.put(id, name.trim());
            }
        }
        if (names.isEmpty()) {
            throw new PlatformUnavailableException(Platform.STEAM, "the tag list came back empty");
        }
        return names;
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

    /**
     * Steam's own "most played" chart (ISteamChartsService). It is public but not part of Valve's
     * documented API, so it could change; the admin seed endpoint also accepts an explicit list of app IDs.
     */
    @Override
    public List<String> fetchMostPlayedGameIds(int limit) {
        Response response = http.get(API + "/ISteamChartsService/GetMostPlayedGames/v1/?key={key}", http.apiKey());
        requireOk(response);

        Map<String, Object> body = SteamJson.map(http.json(response).get("response"));
        List<String> appIds = new ArrayList<>();
        for (Object item : SteamJson.list(body.get("ranks"))) {
            long appId = SteamJson.longValue(SteamJson.map(item).get("appid"));
            if (appId > 0) {
                appIds.add(String.valueOf(appId));
            }
            if (appIds.size() >= limit) {
                break;
            }
        }
        if (appIds.isEmpty()) {
            throw new PlatformUnavailableException(Platform.STEAM,
                    "the most-played chart came back empty; pass explicit appIds instead");
        }
        return appIds;
    }

    /**
     * One page of Steam's full game list (IStoreService/GetAppList, documented by Valve): games only,
     * in ascending appid order. The next page starts after the returned lastId.
     */
    @Override
    public GameIdPage fetchGameIdPage(long afterId, int pageSize) {
        Response response = http.get(API + "/IStoreService/GetAppList/v1/?key={key}"
                        + "&include_games=true&include_dlc=false&include_software=false"
                        + "&include_videos=false&include_hardware=false&max_results={max}&last_appid={last}",
                http.apiKey(), pageSize, afterId);
        requireOk(response);

        Map<String, Object> body = SteamJson.map(http.json(response).get("response"));
        List<String> appIds = new ArrayList<>();
        for (Object item : SteamJson.list(body.get("apps"))) {
            long appId = SteamJson.longValue(SteamJson.map(item).get("appid"));
            if (appId > 0) {
                appIds.add(String.valueOf(appId));
            }
        }
        long lastId = SteamJson.longValue(body.get("last_appid"));
        if (lastId <= 0 && !appIds.isEmpty()) {
            lastId = Long.parseLong(appIds.get(appIds.size() - 1));
        }
        // Steam says "have_more_results"; a full page is also treated as "there may be more".
        boolean hasMore = Boolean.TRUE.equals(body.get("have_more_results")) || appIds.size() >= pageSize;
        return new GameIdPage(appIds, lastId, hasMore);
    }

    private static void requireOk(Response response) {
        if (!response.ok()) {
            throw new PlatformUnavailableException(Platform.STEAM, "unexpected HTTP status " + response.status());
        }
    }
}
