package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformAchievement;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformGameInfo;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.StoreItem;
import com.lavrel.catalog.config.CatalogFilterProperties;
import com.lavrel.catalog.domain.ContentPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/** Small builders for the Steam records, which have many fields the import tests do not care about. */
final class SteamTestData {

    static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private SteamTestData() {
    }

    static SteamGameScreening screening() {
        return new SteamGameScreening(new ContentPolicy(CatalogFilterProperties.defaults(true)), 50);
    }

    /** A safe game with enough reviews, unless the arguments say otherwise. */
    static StoreItem storeItem(String appId, String type, Integer reviewCount, List<Integer> descriptorIds) {
        return new StoreItem(appId, true, type, "Game " + appId, "", List.of(), descriptorIds,
                List.of(), List.of(), List.of(), reviewCount, 90, null,
                true, false, false, null, null, List.of(), List.of(), List.of());
    }

    static StoreItem game(String appId) {
        return storeItem(appId, "game", 1000, List.of());
    }

    static PlatformGameInfo gameInfo(String appId, String type, Map<Integer, String> genres) {
        return new PlatformGameInfo(appId, "Game " + appId, null, "A game", type, List.of(), genres, Map.of(),
                List.of(), false, null, true, false, false, List.of(), List.of());
    }

    static PlatformGameInfo gameInfo(String appId) {
        return gameInfo(appId, "game", Map.of(1, "Action"));
    }

    static List<PlatformAchievement> oneAchievement() {
        return List.of(new PlatformAchievement("ACH_1", "First", "Do it", null, false));
    }
}
