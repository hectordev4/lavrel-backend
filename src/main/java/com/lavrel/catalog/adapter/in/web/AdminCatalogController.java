package com.lavrel.catalog.adapter.in.web;

import com.lavrel.catalog.application.port.in.ImportSteamCatalogUseCase;
import com.lavrel.catalog.application.port.in.ImportSteamCatalogUseCase.BatchSeedResult;
import com.lavrel.catalog.application.port.in.ImportSteamCatalogUseCase.EnrichResult;
import com.lavrel.catalog.application.port.in.ImportSteamCatalogUseCase.RefreshResult;
import com.lavrel.catalog.application.port.in.ImportSteamCatalogUseCase.SeedResult;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin only (see SecurityConfig: /api/admin/** needs an admin Firebase UID).
 * Fills the catalog from Steam (only games that pass the content policy). Calls are slow on purpose.
 *  - /seed: the most played games, or an explicit list of app IDs.
 *  - /seed-next: resumable walk through ALL Steam games; call it again with the returned nextCursor.
 *  - /enrich: gives details to games imported before details existed; adult ones are removed.
 *  - /refresh: the manual sync with Steam for published games (tags, genres, details); games stay visible.
 */
@RestController
@RequestMapping("/api/admin/catalog/steam")
public class AdminCatalogController {

    private static final int DEFAULT_LIMIT = 100;
    private static final int DEFAULT_ENRICH_LIMIT = 50;
    private static final int DEFAULT_REFRESH_DAYS = 30;
    private static final int MAX_LIMIT = 200;

    private final ImportSteamCatalogUseCase importSteamCatalog;

    public AdminCatalogController(ImportSteamCatalogUseCase importSteamCatalog) {
        this.importSteamCatalog = importSteamCatalog;
    }

    /** Body {"limit": 100} for the most played games, or {"appIds": ["570", "730"]} for specific ones. */
    @PostMapping("/seed")
    public SeedResponse seed(@RequestBody(required = false) SeedRequest request) {
        List<String> appIds = request == null || request.appIds() == null ? List.of()
                : request.appIds().stream().map(String::trim).filter(id -> id.matches("\\d{1,10}")).toList();

        SeedResult result;
        if (!appIds.isEmpty()) {
            result = importSteamCatalog.seed(appIds);
        } else {
            result = importSteamCatalog.seedMostPlayed(clamp(request == null ? null : request.limit(), DEFAULT_LIMIT));
        }
        return new SeedResponse(result.requested(), result.alreadyKnown(), result.skippedKnownFailures(),
                result.imported(), result.notImportable(), result.stoppedEarly(), result.pendingEnriched());
    }

    /** Body {"cursor": 0, "limit": 100}. limit = real import attempts for this call. */
    @PostMapping("/seed-next")
    public BatchSeedResponse seedNext(@RequestBody(required = false) SeedNextRequest request) {
        long cursor = request == null || request.cursor() == null ? 0 : Math.max(0, request.cursor());
        BatchSeedResult r = importSteamCatalog.seedNextBatch(cursor,
                clamp(request == null ? null : request.limit(), DEFAULT_LIMIT));
        return new BatchSeedResponse(r.nextCursor(), r.hasMore(), r.scanned(), r.alreadyKnown(),
                r.skippedKnownFailures(), r.attempted(), r.imported(), r.notImportable(), r.stoppedEarly());
    }

    /** Body {"limit": 50}. Run until remaining is 0. */
    @PostMapping("/enrich")
    public EnrichResponse enrich(@RequestBody(required = false) EnrichRequest request) {
        EnrichResult r = importSteamCatalog.enrichPendingGames(
                clamp(request == null ? null : request.limit(), DEFAULT_ENRICH_LIMIT));
        return new EnrichResponse(r.checked(), r.removed(), r.remaining(), r.stoppedEarly());
    }

    /**
     * Body {"appIds": ["292030"]} for specific games, or {"olderThanDays": 30, "limit": 50} for the games not
     * refreshed in that time (or never). {"refreshTagNames": true} also replaces the stored tag names.
     * Run until remaining is 0.
     */
    @PostMapping("/refresh")
    public RefreshResponse refresh(@RequestBody(required = false) RefreshRequest request) {
        List<String> appIds = request == null || request.appIds() == null ? List.of()
                : request.appIds().stream().map(String::trim).filter(id -> id.matches("\\d{1,10}")).toList();
        int olderThanDays = request == null || request.olderThanDays() == null
                ? DEFAULT_REFRESH_DAYS : Math.max(0, Math.min(request.olderThanDays(), 3650));
        boolean refreshTagNames = request != null && Boolean.TRUE.equals(request.refreshTagNames());

        RefreshResult r = importSteamCatalog.refreshGames(appIds, olderThanDays,
                clamp(request == null ? null : request.limit(), DEFAULT_ENRICH_LIMIT), refreshTagNames);
        return new RefreshResponse(r.refreshed(), r.removed(), r.notInCatalog(), r.remaining(), r.stoppedEarly());
    }

    private static int clamp(Integer limit, int defaultValue) {
        return Math.max(1, Math.min(limit == null ? defaultValue : limit, MAX_LIMIT));
    }

    public record SeedRequest(Integer limit, List<String> appIds) {
    }

    public record SeedNextRequest(Long cursor, Integer limit) {
    }

    public record EnrichRequest(Integer limit) {
    }

    public record RefreshRequest(List<String> appIds, Integer olderThanDays, Integer limit, Boolean refreshTagNames) {
    }

    public record SeedResponse(int requested, int alreadyKnown, int skippedKnownFailures, int imported,
                               int notImportable, boolean stoppedEarly, int pendingEnriched) {
    }

    public record BatchSeedResponse(long nextCursor, boolean hasMore, int scanned, int alreadyKnown,
                                    int skippedKnownFailures, int attempted, int imported, int notImportable,
                                    boolean stoppedEarly) {
    }

    public record EnrichResponse(int checked, int removed, int remaining, boolean stoppedEarly) {
    }

    public record RefreshResponse(int refreshed, int removed, int notInCatalog, int remaining, boolean stoppedEarly) {
    }
}
