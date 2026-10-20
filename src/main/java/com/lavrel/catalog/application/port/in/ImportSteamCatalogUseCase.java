package com.lavrel.catalog.application.port.in;

import java.util.List;
import java.util.Map;

/**
 * Builds the SFW catalog from Steam. Every game that enters passes the content policy and gets its details
 * (tags, creators, features, ratings...) at the same time, so public pages can safely show it.
 */
public interface ImportSteamCatalogUseCase {

    /**
     * Imports Steam apps that are not in the catalog yet (used by account syncs): every safe game that has
     * achievements, whatever its popularity. Apps already known, and apps that failed recently (delisted,
     * tools, soundtracks, adult content, no achievements...), cost no Steam call. Only real attempts count
     * toward maxAttempts. If Steam becomes unavailable the run stops.
     *
     * @param steamAppIds in priority order
     */
    ImportOutcome importMissing(List<String> steamAppIds, int maxAttempts);

    /** Imports the given apps, then enriches pending games. */
    SeedResult seed(List<String> steamAppIds);

    /** Imports the most played games on Steam right now, then enriches pending games. */
    SeedResult seedMostPlayed(int limit);

    /**
     * Walks Steam's full game list in id order, starting after afterAppId, and imports up to
     * {@code limit} safe, reasonably popular games that have achievements. Cheap checks happen in bulk first,
     * so only promising games cost per-game calls. Known and already rejected apps cost nothing.
     * Feed nextCursor back as afterAppId to continue until hasMore is false.
     */
    BatchSeedResult seedNextBatch(long afterAppId, int limit);

    /**
     * Gives details to games that were never enriched (games imported before details existed): checks
     * them against the content policy, publishes the safe ones, and removes adult ones and games Steam
     * no longer knows.
     */
    EnrichResult enrichPendingGames(int maxGames);

    /**
     * The manual sync with Steam: re-copies the details of games that are already published (tags, genres,
     * creators, features, ratings, title, description...), replacing what is stored. The games stay visible while
     * this runs, and each one is judged by the content policy again: a game that no longer passes is removed.
     * Tags and genres never change on their own, only through this call.
     *
     * @param steamAppIds     the games to refresh; when empty, the games not refreshed in olderThanDays (or never)
     * @param refreshTagNames also replace the stored tag names with Steam's current ones
     */
    RefreshResult refreshGames(List<String> steamAppIds, int olderThanDays, int limit, boolean refreshTagNames);

    /**
     * imported: steamAppId -> canonical game id, for the apps imported by the call.
     * deferred: apps that could still be imported but were not reached (attempt limit or Steam trouble).
     */
    record ImportOutcome(Map<String, Long> imported, int deferred) {
    }

    record SeedResult(int requested, int alreadyKnown, int skippedKnownFailures, int imported,
                      int notImportable, boolean stoppedEarly, int pendingEnriched) {
    }

    record BatchSeedResult(long nextCursor, boolean hasMore, int scanned, int alreadyKnown,
                           int skippedKnownFailures, int attempted, int imported, int notImportable,
                           boolean stoppedEarly) {
    }

    record EnrichResult(int checked, int removed, int remaining, boolean stoppedEarly) {
    }

    record RefreshResult(int refreshed, int removed, int notInCatalog, int remaining, boolean stoppedEarly) {
    }
}
