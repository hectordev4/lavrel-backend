package com.lavrel.catalog.application.port.in;

import java.util.List;

/**
 * Builds the SFW catalog from Steam. Every game that enters passes the content policy and gets its details
 * (tags, creators, features, ratings...) at the same time, so public pages can safely show it.
 */
public interface ImportSteamCatalogUseCase {

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
