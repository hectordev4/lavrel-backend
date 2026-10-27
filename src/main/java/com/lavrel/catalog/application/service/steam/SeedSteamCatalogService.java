package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.in.EnrichGamesUseCase;
import com.lavrel.catalog.application.port.in.EnrichGamesUseCase.EnrichResult;
import com.lavrel.catalog.application.port.in.SeedSteamCatalogUseCase;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.GameIdPage;
import com.lavrel.shared.domain.PlatformUnavailableException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Admin seeding. An explicit list or the most played games are imported like a sync (popularity not checked),
 * then pending games are enriched. The resumable walk through all of Steam is stricter: it also drops games with
 * too few reviews, and pauses longer between calls.
 */
@Service
public class SeedSteamCatalogService implements SeedSteamCatalogUseCase {

    private static final Logger log = LoggerFactory.getLogger(SeedSteamCatalogService.class);
    private static final int ENRICH_PER_SEED = 100;
    private static final int PAGE_SIZE = 200;
    private static final int MAX_PAGES_PER_BATCH = 25;

    private final PlatformCatalogPort steam;
    private final SteamImportRun importRun;
    private final EnrichGamesUseCase enrichGames;
    private final long delayMs;
    private final long batchDelayMs;

    SeedSteamCatalogService(List<PlatformCatalogPort> catalogPorts, SteamImportRun importRun,
                            EnrichGamesUseCase enrichGames,
                            @Value("${app.catalog.import-delay-ms:300}") long delayMs,
                            @Value("${app.catalog.seed-delay-ms:1000}") long batchDelayMs) {
        this.steam = SteamCatalogPorts.steam(catalogPorts);
        this.importRun = importRun;
        this.enrichGames = enrichGames;
        this.delayMs = delayMs;
        this.batchDelayMs = batchDelayMs;
    }

    @Override
    public SeedResult seed(List<String> steamAppIds) {
        SteamImportRun.Result run = importRun.run(steamAppIds, steamAppIds.size(), ImportMode.SYNC, delayMs);
        EnrichResult enriched = enrichGames.enrichPendingGames(ENRICH_PER_SEED);
        return new SeedResult(run.requested(), run.alreadyKnown(), run.skippedKnownFailures(),
                run.imported().size(), run.notImportable(), run.stoppedEarly(), enriched.checked());
    }

    @Override
    public SeedResult seedMostPlayed(int limit) {
        return seed(steam.fetchMostPlayedGameIds(limit));
    }

    @Override
    public BatchSeedResult seedNextBatch(long afterAppId, int limit) {
        long cursor = afterAppId;
        int budget = limit;
        int scanned = 0;
        int alreadyKnown = 0;
        int skippedFailures = 0;
        int attempted = 0;
        int imported = 0;
        int notImportable = 0;
        boolean stoppedEarly = false;
        boolean hasMore = false;

        for (int pageNo = 0; pageNo < MAX_PAGES_PER_BATCH && budget > 0; pageNo++) {
            GameIdPage page;
            try {
                page = steam.fetchGameIdPage(cursor, PAGE_SIZE);
            } catch (PlatformUnavailableException e) {
                if (scanned == 0) {
                    throw e;                       // nothing done yet: report the failure
                }
                log.warn("Steam unavailable while listing games after {}: {}", cursor, e.getMessage());
                stoppedEarly = true;
                hasMore = true;
                break;
            }
            if (page.ids().isEmpty()) {
                hasMore = false;
                break;
            }

            SteamImportRun.Result run = importRun.run(page.ids(), budget, ImportMode.BULK, batchDelayMs);
            scanned += run.processed();
            alreadyKnown += run.alreadyKnown();
            skippedFailures += run.skippedKnownFailures();
            attempted += run.attempts();
            imported += run.imported().size();
            notImportable += run.notImportable();
            budget -= run.attempts();

            if (run.stoppedEarly() || run.budgetExhausted()) {
                // Continue right after the last app that was fully handled; the rest is retried next call.
                if (run.lastProcessedId() != null) {
                    cursor = Long.parseLong(run.lastProcessedId());
                }
                stoppedEarly = run.stoppedEarly();
                hasMore = true;
                break;
            }
            cursor = page.lastId();
            hasMore = page.hasMore();
            if (!hasMore) {
                break;
            }
        }
        return new BatchSeedResult(cursor, hasMore, scanned, alreadyKnown, skippedFailures, attempted,
                imported, notImportable, stoppedEarly);
    }
}
