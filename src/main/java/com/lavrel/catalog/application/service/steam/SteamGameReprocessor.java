package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.GameRepositoryPort;
import com.lavrel.catalog.application.port.out.ImportFailureRepositoryPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformGameInfo;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.StoreItem;
import com.lavrel.catalog.domain.GameMapping;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformUnavailableException;
import java.time.Clock;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Re-reads the Steam data of games that are already in the catalog: one batched call for the cheap data, one
 * store-page call per game. Games that pass get their details (re)written and are published; the others are
 * removed and the reason is remembered. Enrichment and refresh both use it; they only differ in which games
 * they pick.
 */
@Component
class SteamGameReprocessor {

    private static final Logger log = LoggerFactory.getLogger(SteamGameReprocessor.class);
    private static final int STORE_ITEM_CHUNK = 25;

    private final PlatformCatalogPort steam;
    private final GameRepositoryPort games;
    private final GameMappingRepositoryPort gameMappings;
    private final ImportFailureRepositoryPort failures;
    private final SteamGameScreening screening;
    private final SteamGameWriter writer;
    private final Clock clock;
    private final long delayMs;

    SteamGameReprocessor(List<PlatformCatalogPort> catalogPorts, GameRepositoryPort games,
                         GameMappingRepositoryPort gameMappings, ImportFailureRepositoryPort failures,
                         SteamGameScreening screening, SteamGameWriter writer, Clock clock,
                         @Value("${app.catalog.import-delay-ms:300}") long delayMs) {
        this.steam = SteamCatalogPorts.steam(catalogPorts);
        this.games = games;
        this.gameMappings = gameMappings;
        this.failures = failures;
        this.screening = screening;
        this.writer = writer;
        this.clock = clock;
        this.delayMs = delayMs;
    }

    Result reprocess(List<Long> gameIds) {
        int checked = 0;
        int removed = 0;
        boolean stoppedEarly = false;

        Map<Long, String> appIdByGame = new LinkedHashMap<>();
        for (Long gameId : gameIds) {
            Optional<GameMapping> mapping = gameMappings.findByGameIdAndPlatform(gameId, Platform.STEAM);
            if (mapping.isEmpty()) {
                games.delete(gameId);              // cannot be verified against Steam, so it does not stay
                removed++;
                checked++;
            } else {
                appIdByGame.put(gameId, mapping.get().platformGameId());
            }
        }

        List<String> appIds = List.copyOf(appIdByGame.values());
        Map<String, StoreItem> items = new HashMap<>();
        int fetchedUpTo = 0;
        int index = 0;
        for (Map.Entry<Long, String> entry : appIdByGame.entrySet()) {
            long gameId = entry.getKey();
            String appId = entry.getValue();
            try {
                if (index >= fetchedUpTo) {
                    List<String> chunk = appIds.subList(index, Math.min(appIds.size(), index + STORE_ITEM_CHUNK));
                    items.putAll(steam.fetchStoreItems(chunk));
                    fetchedUpTo = index + chunk.size();
                }
                StoreItem item = items.get(appId);
                PlatformGameInfo info = null;
                Optional<String> rejection = screening.cheapRejection(appId, item, false);
                if (rejection.isEmpty()) {
                    info = steam.fetchGameInfo(appId).orElse(null);
                    rejection = info == null
                            ? Optional.of(SteamGameScreening.NOT_FOUND)
                            : screening.fullRejection(appId, info, item);
                }
                if (rejection.isPresent()) {
                    failures.recordFailure(Platform.STEAM, appId, rejection.get(), clock.instant());
                    games.delete(gameId);
                    removed++;
                } else {
                    writer.republish(gameId, appId, info, item);
                }
            } catch (PlatformUnavailableException e) {
                log.warn("Steam unavailable while processing app {}: {}", appId, e.getMessage());
                stoppedEarly = true;
                break;
            }
            checked++;
            index++;
            SteamCatalogPorts.pause(delayMs);
        }
        return new Result(checked, removed, stoppedEarly);
    }

    record Result(int checked, int removed, boolean stoppedEarly) {
    }
}
