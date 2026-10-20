package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.ImportSteamCatalogUseCase;
import com.lavrel.catalog.application.port.out.AchievementRepositoryPort;
import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.GameMetadataRepositoryPort;
import com.lavrel.catalog.application.port.out.GameRepositoryPort;
import com.lavrel.catalog.application.port.out.ImportFailureRepositoryPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.GameIdPage;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformAchievement;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformGameInfo;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.StoreItem;
import com.lavrel.catalog.domain.Achievement;
import com.lavrel.catalog.domain.ContentPolicy;
import com.lavrel.catalog.domain.Game;
import com.lavrel.catalog.domain.GameFeature;
import com.lavrel.catalog.domain.GameMapping;
import com.lavrel.catalog.domain.GameMetadata;
import com.lavrel.catalog.domain.GameMetadata.Creator;
import com.lavrel.catalog.domain.GameMetadata.Feature;
import com.lavrel.catalog.domain.GameMetadata.FeatureGroup;
import com.lavrel.catalog.domain.GameMetadata.Genre;
import com.lavrel.catalog.domain.GameMetadata.TagWeight;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformUnavailableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Builds the SFW catalog from Steam while spending as few Steam calls as possible.
 *
 * Per game there are two kinds of data:
 *  - a cheap "store item" (tags with weights, creators, reviews, categories...), fetched for 25 games in ONE call;
 *  - the full store page (description, age ratings, category names), one call per game.
 * Games are first judged on the cheap data (is it a game, is it adult, is it popular enough) and only the
 * survivors cost per-game calls: an achievement check, then the store page.
 *  - Apps already in the catalog, and apps that failed before, cost nothing until the retry window passes.
 *  - Transient failures (Steam down, rate limit) are never remembered as permanent failures.
 * Network calls run outside database transactions; a game and its details are written in one transaction.
 */
@Service
public class ImportSteamCatalogService implements ImportSteamCatalogUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportSteamCatalogService.class);
    private static final int ENRICH_PER_SEED = 100;
    private static final int PAGE_SIZE = 200;
    private static final int MAX_PAGES_PER_BATCH = 25;
    private static final int STORE_ITEM_CHUNK = 25;

    // Failure reasons. TOO_FEW_REVIEWS is only applied by bulk seeding; syncs and explicit lists ignore it.
    static final String NO_ACHIEVEMENTS = "no achievements";
    static final String TOO_FEW_REVIEWS = "too few reviews";
    static final String NOT_FOUND = "not found in the Steam store";

    private final PlatformCatalogPort steam;
    private final GameRepositoryPort games;
    private final GameMappingRepositoryPort gameMappings;
    private final AchievementRepositoryPort achievements;
    private final ImportFailureRepositoryPort failures;
    private final GameMetadataRepositoryPort metadata;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final ContentPolicy contentPolicy;
    private final int failureRetryDays;
    private final long delayMs;
    private final long batchDelayMs;
    private final Mode syncMode = new Mode(true, 0, Set.of(TOO_FEW_REVIEWS));
    private final Mode bulkMode;

    private volatile Instant tagNamesRefreshedAt = Instant.EPOCH;

    public ImportSteamCatalogService(List<PlatformCatalogPort> catalogPorts, GameRepositoryPort games,
                                     GameMappingRepositoryPort gameMappings,
                                     AchievementRepositoryPort achievements,
                                     ImportFailureRepositoryPort failures,
                                     GameMetadataRepositoryPort metadata,
                                     TransactionTemplate tx, Clock clock,
                                     @Value("${app.catalog.content-filter:standard}") String contentFilter,
                                     @Value("${app.catalog.sexual-tag-top-rank:5}") int sexualTagTopRank,
                                     @Value("${app.catalog.sexual-tag-weight-ratio:0.6}") double sexualTagWeightRatio,
                                     @Value("${app.catalog.min-reviews:50}") int minReviews,
                                     @Value("${app.catalog.failure-retry-days:30}") int failureRetryDays,
                                     @Value("${app.catalog.import-delay-ms:300}") long delayMs,
                                     @Value("${app.catalog.seed-delay-ms:1000}") long batchDelayMs) {
        this.steam = catalogPorts.stream()
                .filter(p -> p.platform() == Platform.STEAM)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Steam catalog adapter registered"));
        this.games = games;
        this.gameMappings = gameMappings;
        this.achievements = achievements;
        this.failures = failures;
        this.metadata = metadata;
        this.tx = tx;
        this.clock = clock;
        ContentPolicy.Level level = ContentPolicy.Level.parse(contentFilter);
        this.contentPolicy = new ContentPolicy(level, sexualTagTopRank, sexualTagWeightRatio);
        this.failureRetryDays = failureRetryDays;
        this.delayMs = delayMs;
        this.batchDelayMs = batchDelayMs;
        this.bulkMode = new Mode(true, minReviews, Set.of());
        log.info("Catalog content filter: {} (sexual tags prominent in the top {} or >= {} of the top weight)",
                level, sexualTagTopRank, sexualTagWeightRatio);
    }

    @Override
    public ImportOutcome importMissing(List<String> steamAppIds, int maxAttempts) {
        ImportRun run = run(steamAppIds, maxAttempts, syncMode, delayMs);
        return new ImportOutcome(run.imported(), run.deferred());
    }

    @Override
    public SeedResult seed(List<String> steamAppIds) {
        ImportRun run = run(steamAppIds, steamAppIds.size(), syncMode, delayMs);
        EnrichResult enriched = enrichPendingGames(ENRICH_PER_SEED);
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

            ImportRun run = run(page.ids(), budget, bulkMode, batchDelayMs);
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

    @Override
    public EnrichResult enrichPendingGames(int maxGames) {
        Processed done = processGames(metadata.findPendingGameIds(maxGames));
        return new EnrichResult(done.checked(), done.removed(), metadata.countPending(), done.stoppedEarly());
    }

    @Override
    public RefreshResult refreshGames(List<String> steamAppIds, int olderThanDays, int limit,
                                      boolean refreshTagNames) {
        if (refreshTagNames) {
            try {
                metadata.saveTagNames(steam.fetchTagNames(), true);
            } catch (PlatformUnavailableException e) {
                log.warn("Could not refresh Steam's tag names: {}", e.getMessage());
            }
        }
        Instant olderThan = clock.instant().minus(Duration.ofDays(olderThanDays));

        List<Long> gameIds = new ArrayList<>();
        int notInCatalog = 0;
        if (steamAppIds.isEmpty()) {
            gameIds.addAll(metadata.findStaleGameIds(olderThan, limit));
        } else {
            List<GameMapping> mappings = gameMappings.findByPlatformAndPlatformGameIds(Platform.STEAM, steamAppIds);
            mappings.forEach(m -> gameIds.add(m.gameId()));
            notInCatalog = steamAppIds.size() - mappings.size();
        }
        Processed done = processGames(gameIds);
        return new RefreshResult(done.checked() - done.removed(), done.removed(), notInCatalog,
                metadata.countStale(olderThan), done.stoppedEarly());
    }

    /**
     * Re-reads the Steam data of existing games: one batched call for the cheap data, one store-page call per game.
     * Games that pass get their details (re)written and are published; the others are removed.
     * Used both for games never enriched and for the manual refresh.
     */
    private Processed processGames(List<Long> gameIds) {
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
                Optional<String> rejection = cheapRejection(item, syncMode);
                if (rejection.isEmpty()) {
                    info = steam.fetchGameInfo(appId).orElse(null);
                    rejection = info == null ? Optional.of(NOT_FOUND) : fullRejection(info, item);
                }
                if (rejection.isPresent()) {
                    failures.recordFailure(Platform.STEAM, appId, rejection.get(), clock.instant());
                    games.delete(gameId);
                    removed++;
                } else {
                    publish(gameId, appId, info, item);
                }
            } catch (PlatformUnavailableException e) {
                log.warn("Steam unavailable while processing app {}: {}", appId, e.getMessage());
                stoppedEarly = true;
                break;
            }
            checked++;
            index++;
            pause(delayMs);
        }
        return new Processed(checked, removed, stoppedEarly);
    }

    // ------------------------------------------------------------------ internals

    /**
     * Goes through the ids in order. Known apps and remembered failures are free. For the others, the cheap
     * store item is fetched (25 at a time) and judged; only survivors are real attempts, up to maxAttempts.
     * lastProcessedId is the last app fully handled, so a caller can continue after it.
     */
    private ImportRun run(List<String> requestedIds, int maxAttempts, Mode mode, long pauseMs) {
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
                Optional<String> cheapRejection = cheapRejection(item, mode);
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
                        Optional<Long> gameId = importOne(appId, item, mode);
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
                    pause(pauseMs);
                }
                candidateIndex++;
            }
            processed++;
            lastProcessed = appId;
        }
        int deferred = budgetExhausted || stoppedEarly ? candidates.size() - candidateIndex : 0;
        return new ImportRun(ids.size(), alreadyKnown, skippedFailures, attempts, processed, imported,
                notImportable, stoppedEarly, budgetExhausted, lastProcessed, deferred);
    }

    /** Judges a game on the cheap store item alone. @return the reason it must stay out, if any */
    private Optional<String> cheapRejection(StoreItem item, Mode mode) {
        if (item == null || !item.found()) {
            return Optional.of(NOT_FOUND);
        }
        if (!"game".equals(item.type())) {
            return Optional.of("type: " + item.type());
        }
        Optional<String> policy = contentPolicy.rejectionReason(new ContentPolicy.Signals(
                item.name(), item.shortDescription(), item.contentDescriptorIds(), List.of(),
                item.tags(), item.esrbRating()));
        if (policy.isPresent()) {
            return policy;
        }
        if (mode.minReviews() > 0 && (item.reviewCount() == null || item.reviewCount() < mode.minReviews())) {
            return Optional.of(TOO_FEW_REVIEWS);
        }
        return Optional.empty();
    }

    /** Judges a game again with the full store page (genres, descriptors, description wording). */
    private Optional<String> fullRejection(PlatformGameInfo info, StoreItem item) {
        if (info.type() != null && !"game".equals(info.type())) {
            return Optional.of("type: " + info.type());
        }
        return contentPolicy.rejectionReason(new ContentPolicy.Signals(
                info.title(), info.description(), info.contentDescriptorIds(), info.genres().values(),
                item == null ? List.of() : item.tags(),
                item == null ? null : item.esrbRating()));
    }

    /** @return the new game id, or empty if the app is not importable (the reason is remembered) */
    private Optional<Long> importOne(String appId, StoreItem item, Mode mode) {
        Instant now = clock.instant();
        List<PlatformAchievement> steamAchievements = null;

        if (mode.requireAchievements()) {
            // Cheapest per-game check first: the achievement schema comes from the generous API.
            steamAchievements = steam.fetchAchievements(appId);
            if (steamAchievements.isEmpty()) {
                failures.recordFailure(Platform.STEAM, appId, NO_ACHIEVEMENTS, now);
                return Optional.empty();
            }
        }

        PlatformGameInfo info = steam.fetchGameInfo(appId).orElse(null);
        if (info == null) {
            failures.recordFailure(Platform.STEAM, appId, NOT_FOUND, now);
            return Optional.empty();
        }
        Optional<String> rejection = fullRejection(info, item);
        if (rejection.isPresent()) {
            failures.recordFailure(Platform.STEAM, appId, rejection.get(), now);
            return Optional.empty();
        }
        if (steamAchievements == null) {
            steamAchievements = steam.fetchAchievements(appId);
        }

        GameMetadata gameMetadata = buildMetadata(info, item);
        ensureTagNames(gameMetadata.tags());
        List<PlatformAchievement> toStore = steamAchievements;

        Long gameId = tx.execute(status -> {
            Game game = games.save(new Game(null, info.title(), info.coverImageUrl(),
                    nullToEmpty(info.description()), storeUrl(appId), now));
            gameMappings.save(new GameMapping(null, game.id(), Platform.STEAM, appId, info.title()));
            achievements.saveAll(toStore.stream()
                    .map(a -> new Achievement(null, game.id(), a.platformAchievementId(), a.name(),
                            a.description(), a.iconUrl(), a.hidden(), now))
                    .toList());
            metadata.save(game.id(), gameMetadata);          // publishes the game
            return game.id();
        });
        return Optional.ofNullable(gameId);
    }

    /** Writes the details of an existing game (publishing it if it was pending). Also used by the manual refresh. */
    private void publish(long gameId, String appId, PlatformGameInfo info, StoreItem item) {
        Game game = games.findById(gameId).orElse(null);
        if (game == null) {
            return;
        }
        GameMetadata gameMetadata = buildMetadata(info, item);
        ensureTagNames(gameMetadata.tags());
        tx.executeWithoutResult(status -> {
            games.save(new Game(game.id(), info.title(), info.coverImageUrl(), nullToEmpty(info.description()),
                    storeUrl(appId), game.createdAt()));
            metadata.save(gameId, gameMetadata);
        });
    }

    private GameMetadata buildMetadata(PlatformGameInfo info, StoreItem item) {
        LocalDate release = null;
        if (item != null && item.releaseEpochSeconds() != null && item.releaseEpochSeconds() > 0) {
            release = Instant.ofEpochSecond(item.releaseEpochSeconds()).atZone(ZoneOffset.UTC).toLocalDate();
        }
        List<Creator> developers = item != null && !item.developers().isEmpty()
                ? item.developers() : info.developers().stream().map(name -> new Creator(name, null)).toList();
        List<Creator> publishers = item != null && !item.publishers().isEmpty()
                ? item.publishers() : info.publishers().stream().map(name -> new Creator(name, null)).toList();

        boolean windows = item != null ? item.windows() : info.windows();
        boolean mac = item != null ? item.mac() : info.mac();
        boolean linux = item != null ? item.linux() : info.linux();
        Integer requiredAge = item != null && item.requiredAge() != null ? item.requiredAge() : info.requiredAge();

        return new GameMetadata(release, windows, mac, linux,
                item == null ? null : item.reviewCount(),
                item == null ? null : item.reviewPercentPositive(),
                info.free(), requiredAge, developers, publishers,
                item == null ? List.of() : item.franchises(),
                item == null ? List.of() : item.tags(),
                info.genres().entrySet().stream().map(e -> new Genre(e.getKey(), e.getValue())).toList(),
                buildFeatures(info, item), info.ageRatings());
    }

    /**
     * The store item says which group each category belongs to; the store page gives their names.
     * Categories only the store page knows are added as plain features.
     */
    private static List<Feature> buildFeatures(PlatformGameInfo info, StoreItem item) {
        Map<Integer, Feature> byId = new LinkedHashMap<>();
        if (item != null) {
            addFeatures(byId, item.playerCategoryIds(), FeatureGroup.PLAYER_MODE, info);
            addFeatures(byId, item.featureCategoryIds(), FeatureGroup.FEATURE, info);
            addFeatures(byId, item.controllerCategoryIds(), FeatureGroup.CONTROLLER, info);
        }
        info.categories().forEach((id, name) ->
                byId.putIfAbsent(id, new Feature(id, name, groupOf(id, FeatureGroup.FEATURE))));
        return List.copyOf(byId.values());
    }

    private static void addFeatures(Map<Integer, Feature> byId, List<Integer> ids, FeatureGroup listGroup,
                                    PlatformGameInfo info) {
        for (Integer id : ids) {
            byId.putIfAbsent(id, new Feature(id, info.categories().get(id), groupOf(id, listGroup)));
        }
    }

    /** Known features keep their own group (accessibility items arrive inside the "features" list). */
    private static FeatureGroup groupOf(int categoryId, FeatureGroup fallback) {
        return GameFeature.byCategoryId(categoryId).map(GameFeature::group).orElse(fallback);
    }

    /** Loads Steam's tag names when a game has a tag we have no name for (at most once an hour). */
    private void ensureTagNames(List<TagWeight> tags) {
        Set<Long> ids = tags.stream().map(TagWeight::tagId).collect(Collectors.toSet());
        if (ids.isEmpty() || metadata.findKnownTagIds(ids).containsAll(ids)) {
            return;
        }
        Instant now = clock.instant();
        if (Duration.between(tagNamesRefreshedAt, now).compareTo(Duration.ofHours(1)) < 0) {
            return;
        }
        tagNamesRefreshedAt = now;
        try {
            metadata.saveTagNames(steam.fetchTagNames(), false);   // only fills placeholders, never renames
        } catch (PlatformUnavailableException e) {
            log.warn("Could not load Steam's tag names: {}", e.getMessage());   // placeholders are used meanwhile
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String storeUrl(String steamAppId) {
        return "https://store.steampowered.com/app/" + steamAppId;
    }

    /** Small pause between Steam calls: the store API is rate limited. */
    private static void pause(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Syncs and explicit lists import every safe game that has achievements. Bulk seeding also drops
     * games with too few reviews, so a walk through all of Steam stays useful.
     */
    private record Mode(boolean requireAchievements, int minReviews, Set<String> ignoredFailureReasons) {
    }

    private record Processed(int checked, int removed, boolean stoppedEarly) {
    }

    private record ImportRun(int requested, int alreadyKnown, int skippedKnownFailures, int attempts,
                             int processed, Map<String, Long> imported, int notImportable,
                             boolean stoppedEarly, boolean budgetExhausted, String lastProcessedId,
                             int deferred) {
    }
}
