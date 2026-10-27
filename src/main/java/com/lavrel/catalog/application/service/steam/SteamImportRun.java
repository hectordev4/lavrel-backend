package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.ImportFailureRepositoryPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformAchievement;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformGameInfo;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.StoreItem;
import com.lavrel.catalog.domain.GameMapping;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformUnavailableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Imports a list of Steam apps that may or may not be in the catalog, spending as few Steam calls as possible.
 * Used by account syncs and by every kind of seeding.
 *  - Apps already in the catalog, and apps that failed recently, cost nothing.
 *  - The others get the cheap store item (25 per call) and are judged on it; rejections are remembered.
 *  - Only the survivors are real attempts, up to maxAttempts: achievement check, store page, full check, write.
 *  - Transient failures (Steam down, rate limit) stop the run and are never remembered as failures.
 */
@Component
class SteamImportRun {

    private static final Logger log = LoggerFactory.getLogger(SteamImportRun.class);
    private static final int STORE_ITEM_CHUNK = 25;

    private final PlatformCatalogPort steam;
    private final GameMappingRepositoryPort gameMappings;
    private final ImportFailureRepositoryPort failures;
    private final SteamGameScreening screening;
    private final SteamGameWriter writer;
    private final Clock clock;
    private final int failureRetryDays;

    SteamImportRun(List<PlatformCatalogPort> catalogPorts, GameMappingRepositoryPort gameMappings,
                   ImportFailureRepositoryPort failures, SteamGameScreening screening, SteamGameWriter writer,
                   Clock clock, @Value("${app.catalog.failure-retry-days:30}") int failureRetryDays) {
        this.steam = SteamCatalogPorts.steam(catalogPorts);
        this.gameMappings = gameMappings;
        this.failures = failures;
        this.screening = screening;
        this.writer = writer;
        this.clock = clock;
        this.failureRetryDays = failureRetryDays;
    }

    /**
     * Goes through the ids in order. lastProcessedId is the last app fully handled, so a caller can continue
     * right after it; deferred counts the candidates not reached because of the limit or a Steam failure.
     */
    Result run(List<String> requestedIds, int maxAttempts, ImportMode mode, long pauseMs) {
        List<String> ids = requestedIds.stream().distinct().toList();
        Instant now = clock.instant();

        Set<String> known = gameMappings.findByPlatformAndPlatformGameIds(Platform.STEAM, ids).stream()
                .map(GameMapping::platformGameId)
                .collect(Collectors.toSet());
        List<String> unknown = ids.stream().filter(id -> !known.contains(id)).toList();
        Set<String> knownFailures = failures.findFailedSince(Platform.STEAM, unknown,
                now.minus(Duration.ofDays(failureRetryDays)), mode.ignoredFailureReasons());
        List<String> candidates = unknown.stream().filter(id -> !knownFailures.contains(id)).toList();

        Map<String, StoreItem> items = new HashMap<>();
        Map<String, Long> imported = new LinkedHashMap<>();
        int fetchedUpTo = 0;
        int candidateIndex = 0;
        int attempts = 0;
        int processed = 0;
        int alreadyKnown = 0;
        int skippedFailures = 0;
        int notImportable = 0;
        boolean stoppedEarly = false;
        boolean budgetExhausted = false;
        String lastProcessed = null;

        for (String appId : ids) {
            if (known.contains(appId)) {
                alreadyKnown++;
            } else if (knownFailures.contains(appId)) {
                skippedFailures++;                           // free: no Steam call
            } else {
                if (candidateIndex >= fetchedUpTo) {
                    List<String> chunk = candidates.subList(candidateIndex,
                            Math.min(candidates.size(), candidateIndex + STORE_ITEM_CHUNK));
                    try {
                        items.putAll(steam.fetchStoreItems(chunk));
                    } catch (PlatformUnavailableException e) {
                        log.warn("Steam unavailable while fetching store items: {}", e.getMessage());
                        stoppedEarly = true;
                        break;
                    }
                    fetchedUpTo = candidateIndex + chunk.size();
                }
                StoreItem item = items.get(appId);
                Optional<String> cheapRejection = screening.cheapRejection(appId, item, mode.popularityRequired());
                if (cheapRejection.isPresent()) {
                    failures.recordFailure(Platform.STEAM, appId, cheapRejection.get(), now);
                    notImportable++;
                } else {
                    if (attempts >= maxAttempts) {
                        budgetExhausted = true;
                        break;
                    }
                    attempts++;
                    try {
                        Optional<Long> gameId = importOne(appId, item);
                        if (gameId.isPresent()) {
                            imported.put(appId, gameId.get());
                        } else {
                            notImportable++;
                        }
                    } catch (PlatformUnavailableException e) {
                        log.warn("Steam unavailable while importing app {}: {}", appId, e.getMessage());
                        stoppedEarly = true;
                        break;
                    }
                    SteamCatalogPorts.pause(pauseMs);
                }
                candidateIndex++;
            }
            processed++;
            lastProcessed = appId;
        }
        int deferred = budgetExhausted || stoppedEarly ? candidates.size() - candidateIndex : 0;
        return new Result(ids.size(), alreadyKnown, skippedFailures, attempts, processed, imported,
                notImportable, stoppedEarly, budgetExhausted, lastProcessed, deferred);
    }

    /**
     * Cheapest per-game check first (the achievement schema comes from the generous API), then the store page.
     * @return the new game id, or empty if the app is not importable (the reason is remembered)
     */
    private Optional<Long> importOne(String appId, StoreItem item) {
        Instant now = clock.instant();
        List<PlatformAchievement> steamAchievements = steam.fetchAchievements(appId);
        if (steamAchievements.isEmpty()) {
            failures.recordFailure(Platform.STEAM, appId, SteamGameScreening.NO_ACHIEVEMENTS, now);
            return Optional.empty();
        }
        PlatformGameInfo info = steam.fetchGameInfo(appId).orElse(null);
        if (info == null) {
            failures.recordFailure(Platform.STEAM, appId, SteamGameScreening.NOT_FOUND, now);
            return Optional.empty();
        }
        Optional<String> rejection = screening.fullRejection(appId, info, item);
        if (rejection.isPresent()) {
            failures.recordFailure(Platform.STEAM, appId, rejection.get(), now);
            return Optional.empty();
        }
        return Optional.ofNullable(writer.insert(appId, info, item, steamAchievements));
    }

    /**
     * requested: distinct ids asked for. processed: ids fully handled. attempts: real per-game imports tried.
     * imported: steamAppId -> new game id.
     */
    record Result(int requested, int alreadyKnown, int skippedKnownFailures, int attempts, int processed,
                  Map<String, Long> imported, int notImportable, boolean stoppedEarly, boolean budgetExhausted,
                  String lastProcessedId, int deferred) {
    }
}
