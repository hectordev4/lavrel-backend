package com.lavrel.catalog.adapter.in.web;

import com.lavrel.catalog.application.port.in.EnrichGamesUseCase;
import com.lavrel.catalog.application.port.in.EnrichGamesUseCase.EnrichResult;
import com.lavrel.catalog.application.port.in.RefreshGamesUseCase;
import com.lavrel.catalog.application.port.in.RefreshGamesUseCase.RefreshResult;
import com.lavrel.catalog.application.port.in.SeedSteamCatalogUseCase;
import com.lavrel.catalog.application.port.in.SeedSteamCatalogUseCase.BatchSeedResult;
import com.lavrel.catalog.application.port.in.SeedSteamCatalogUseCase.SeedResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
 * Bodies are validated: an out-of-range limit or a non-numeric app id is a 400 validation_failed.
 */
@RestController
@RequestMapping("/api/admin/catalog/steam")
public class AdminCatalogController {

    private static final int DEFAULT_LIMIT = 100;
    private static final int DEFAULT_ENRICH_LIMIT = 50;
    private static final int DEFAULT_REFRESH_DAYS = 30;
    private static final int MAX_LIMIT = 200;
    private static final int MAX_APP_IDS = 500;
    private static final String APP_ID = "\\d{1,10}";
    private static final String APP_ID_MESSAGE = "must be a numeric Steam app id";

    private final SeedSteamCatalogUseCase seedCatalog;
    private final EnrichGamesUseCase enrichGames;
    private final RefreshGamesUseCase refreshGames;

    public AdminCatalogController(SeedSteamCatalogUseCase seedCatalog, EnrichGamesUseCase enrichGames,
                                  RefreshGamesUseCase refreshGames) {
        this.seedCatalog = seedCatalog;
        this.enrichGames = enrichGames;
        this.refreshGames = refreshGames;
    }

    /** Body {"limit": 100} for the most played games, or {"appIds": ["570", "730"]} for specific ones. */
    @PostMapping("/seed")
    public SeedResponse seed(@Valid @RequestBody(required = false) SeedRequest request) {
        SeedRequest r = request == null ? new SeedRequest(null, null) : request;
        SeedResult result = r.appIds() == null || r.appIds().isEmpty()
                ? seedCatalog.seedMostPlayed(orDefault(r.limit(), DEFAULT_LIMIT))
                : seedCatalog.seed(r.appIds());
        return new SeedResponse(result.requested(), result.alreadyKnown(), result.skippedKnownFailures(),
                result.imported(), result.notImportable(), result.stoppedEarly(), result.pendingEnriched());
    }

    /** Body {"cursor": 0, "limit": 100}. limit = real import attempts for this call. */
    @PostMapping("/seed-next")
    public BatchSeedResponse seedNext(@Valid @RequestBody(required = false) SeedNextRequest request) {
        SeedNextRequest r = request == null ? new SeedNextRequest(null, null) : request;
        BatchSeedResult result = seedCatalog.seedNextBatch(r.cursor() == null ? 0 : r.cursor(),
                orDefault(r.limit(), DEFAULT_LIMIT));
        return new BatchSeedResponse(result.nextCursor(), result.hasMore(), result.scanned(), result.alreadyKnown(),
                result.skippedKnownFailures(), result.attempted(), result.imported(), result.notImportable(),
                result.stoppedEarly());
    }

    /** Body {"limit": 50}. Run until remaining is 0. */
    @PostMapping("/enrich")
    public EnrichResponse enrich(@Valid @RequestBody(required = false) EnrichRequest request) {
        EnrichResult result = enrichGames.enrichPendingGames(
                orDefault(request == null ? null : request.limit(), DEFAULT_ENRICH_LIMIT));
        return new EnrichResponse(result.checked(), result.removed(), result.remaining(), result.stoppedEarly());
    }

    /**
     * Body {"appIds": ["292030"]} for specific games, or {"olderThanDays": 30, "limit": 50} for the games not
     * refreshed in that time (or never). {"refreshTagNames": true} also replaces the stored tag names.
     * Run until remaining is 0.
     */
    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody(required = false) RefreshRequest request) {
        RefreshRequest r = request == null ? new RefreshRequest(null, null, null, null) : request;
        RefreshResult result = refreshGames.refreshGames(
                r.appIds() == null ? List.of() : r.appIds(),
                orDefault(r.olderThanDays(), DEFAULT_REFRESH_DAYS),
                orDefault(r.limit(), DEFAULT_ENRICH_LIMIT),
                Boolean.TRUE.equals(r.refreshTagNames()));
        return new RefreshResponse(result.refreshed(), result.removed(), result.notInCatalog(), result.remaining(),
                result.stoppedEarly());
    }

    private static int orDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    // ---------------------------------------------------------------- request bodies (validated)

    public record SeedRequest(
            @Min(1) @Max(MAX_LIMIT) Integer limit,
            @Size(max = MAX_APP_IDS) List<@NotNull @Pattern(regexp = APP_ID, message = APP_ID_MESSAGE) String> appIds) {
    }

    public record SeedNextRequest(@PositiveOrZero Long cursor, @Min(1) @Max(MAX_LIMIT) Integer limit) {
    }

    public record EnrichRequest(@Min(1) @Max(MAX_LIMIT) Integer limit) {
    }

    public record RefreshRequest(
            @Size(max = MAX_APP_IDS) List<@NotNull @Pattern(regexp = APP_ID, message = APP_ID_MESSAGE) String> appIds,
            @Min(0) @Max(3650) Integer olderThanDays,
            @Min(1) @Max(MAX_LIMIT) Integer limit,
            Boolean refreshTagNames) {
    }

    // ---------------------------------------------------------------- responses

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
