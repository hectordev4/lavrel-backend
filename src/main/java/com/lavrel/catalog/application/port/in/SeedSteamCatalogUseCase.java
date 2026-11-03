package com.lavrel.catalog.application.port.in;

import java.util.List;

/**
 * Fills the catalog from Steam (admin). Every game that enters passes the content policy and has achievements;
 * its details are written at the same time, so public pages can show it straight away.
 */
public interface SeedSteamCatalogUseCase {

    /** Imports the given apps (popularity is not checked), then enriches pending games. */
    SeedResult seed(List<String> steamAppIds);

    /** Imports the most played games on Steam right now, then enriches pending games. */
    SeedResult seedMostPlayed(int limit);

    /**
     * Walks Steam's full game list in id order, starting after afterAppId, and imports up to {@code limit} safe,
     * reasonably popular games that have achievements. Cheap checks happen in bulk first, so only promising games
     * cost per-game calls. Known and already rejected apps cost nothing.
     * Feed nextCursor back as afterAppId to continue until hasMore is false.
     */
    BatchSeedResult seedNextBatch(long afterAppId, int limit);

    record SeedResult(int requested, int alreadyKnown, int skippedKnownFailures, int imported,
                      int notImportable, boolean stoppedEarly, int pendingEnriched) {
    }

    record BatchSeedResult(long nextCursor, boolean hasMore, int scanned, int alreadyKnown,
                           int skippedKnownFailures, int attempted, int imported, int notImportable,
                           boolean stoppedEarly) {
    }
}
