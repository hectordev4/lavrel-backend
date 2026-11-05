package com.lavrel.library.application.service;

import com.lavrel.catalog.application.port.in.ImportPlatformGamesUseCase.ImportOutcome;
import com.lavrel.catalog.application.port.in.ImportPlatformGamesUseCase;
import com.lavrel.catalog.application.port.in.ResolveAchievementUseCase;
import com.lavrel.catalog.application.port.in.ResolveGameUseCase;
import com.lavrel.library.application.port.in.SyncAccountUseCase;
import com.lavrel.library.application.port.out.AccountGameRepositoryPort;
import com.lavrel.library.application.port.out.PlatformLibraryPort.PlatformGame;
import com.lavrel.library.application.port.out.PlatformLibraryPort.PlatformUnlock;
import com.lavrel.library.application.port.out.PlatformLibraryPort;
import com.lavrel.library.domain.AccountGame;
import com.lavrel.progress.application.port.in.RecordUnlocksUseCase.Unlock;
import com.lavrel.progress.application.port.in.RecordUnlocksUseCase;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformRegistry;
import com.lavrel.user.application.port.in.GetPlatformAccountUseCase.PlatformAccountView;
import com.lavrel.user.application.port.in.GetPlatformAccountUseCase;
import com.lavrel.user.application.port.in.MarkAccountSyncedUseCase;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Orchestrates a sync. Platform data is translated to our IDs here, through other features'
 * inbound ports only. Network calls never run inside a database transaction.
 *
 * The platform's library adapter is picked through a PlatformRegistry, so this class never names a platform.
 * Games we do not know yet are handed to the catalog importer, most played first, at most
 * app.sync.max-imports-per-sync real attempts per sync (platforms that cannot feed the catalog import nothing). The importer leaves out adult games and games
 * without achievements, and remembers what it rejected, so those cost no Steam calls on later syncs. What is
 * left over because of the limit is reported as gamesPending: the user syncs again to continue.
 * When the library was read, the account's lastSyncedAt is updated.
 *
 * Two modes: the manual sync reads the unlocks of every owned game; the automatic one (syncChangedGames) only reads
 * them for games that are new to the account or whose playtime went up since the last sync, because nothing can be
 * unlocked without playing. That makes it a handful of Steam calls instead of one per game.
 */
@Service
public class SyncAccountService implements SyncAccountUseCase {

    private final PlatformRegistry<PlatformLibraryPort> libraries;
    private final GetPlatformAccountUseCase accounts;
    private final ResolveGameUseCase gameResolver;
    private final ResolveAchievementUseCase achievementResolver;
    private final ImportPlatformGamesUseCase catalogImporter;
    private final RecordUnlocksUseCase unlockRecorder;
    private final MarkAccountSyncedUseCase accountMarker;
    private final AccountGameRepositoryPort accountGames;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final int maxImportsPerSync;

    public SyncAccountService(List<PlatformLibraryPort> ports, GetPlatformAccountUseCase accounts,
                              ResolveGameUseCase gameResolver, ResolveAchievementUseCase achievementResolver,
                              ImportPlatformGamesUseCase catalogImporter,
                              RecordUnlocksUseCase unlockRecorder, MarkAccountSyncedUseCase accountMarker,
                              AccountGameRepositoryPort accountGames,
                              TransactionTemplate tx, Clock clock,
                              @Value("${app.sync.max-imports-per-sync:40}") int maxImportsPerSync) {
        this.libraries = new PlatformRegistry<>(ports, "library sync");
        this.accounts = accounts;
        this.gameResolver = gameResolver;
        this.achievementResolver = achievementResolver;
        this.catalogImporter = catalogImporter;
        this.unlockRecorder = unlockRecorder;
        this.accountMarker = accountMarker;
        this.accountGames = accountGames;
        this.tx = tx;
        this.clock = clock;
        this.maxImportsPerSync = maxImportsPerSync;
    }

    @Override
    public SyncResult sync(long userId, Platform platform, String externalId) {
        return run(accounts.getOwned(userId, platform, externalId), false);
    }

    @Override
    public SyncResult syncChangedGames(long platformAccountId) {
        return run(accounts.getById(platformAccountId), true);
    }

    private SyncResult run(PlatformAccountView account, boolean changedOnly) {
        long platformAccountId = account.id();
        PlatformLibraryPort library = libraries.require(account.platform());

        Instant now = clock.instant();
        List<PlatformGame> owned = new ArrayList<>(library.fetchOwnedGames(account.externalId()));
        owned.sort(Comparator.comparingInt(PlatformGame::playtimeMinutes).reversed());   // most played first

        Map<String, Long> knownGameIds = gameResolver.resolveGameIds(account.platform(),
                owned.stream().map(PlatformGame::platformGameId).toList());
        List<String> unknownGameIds = owned.stream()
                .map(PlatformGame::platformGameId)
                .filter(id -> !knownGameIds.containsKey(id))
                .toList();
        ImportOutcome outcome = catalogImporter.importMissing(account.platform(), unknownGameIds, maxImportsPerSync);
        Map<String, Long> importedGameIds = outcome.imported();
        int pending = outcome.deferred();

        Map<Long, Integer> previousPlaytime = changedOnly ? previousPlaytime(platformAccountId) : Map.of();
        List<AccountGame> toSave = new ArrayList<>();
        int skipped = 0;
        int unlocksRecorded = 0;

        for (PlatformGame game : owned) {
            Long gameId = knownGameIds.get(game.platformGameId());
            if (gameId == null) {
                gameId = importedGameIds.get(game.platformGameId());
            }
            if (gameId == null) {
                skipped++;           // adult, no achievements, or waiting for a later sync
                continue;
            }
            toSave.add(new AccountGame(platformAccountId, gameId, game.playtimeMinutes(),
                    game.lastPlayedAt(), now));
            if (!changedOnly || playedSinceLastSync(previousPlaytime.get(gameId), game.playtimeMinutes())) {
                unlocksRecorded += syncUnlocks(library, account, game.platformGameId(), gameId);
            }
        }

        tx.executeWithoutResult(status -> accountGames.saveAll(toSave));
        accountMarker.markSynced(platformAccountId);
        return new SyncResult(toSave.size(), skipped, unlocksRecorded, pending);
    }

    private Map<Long, Integer> previousPlaytime(long platformAccountId) {
        Map<Long, Integer> playtime = new HashMap<>();
        for (AccountGame known : accountGames.findByPlatformAccountId(platformAccountId)) {
            playtime.put(known.gameId(), known.playtimeMinutes());
        }
        return playtime;
    }

    /** New to the account (never read) or played more since: worth asking the platform for its unlocks. */
    static boolean playedSinceLastSync(Integer previousMinutes, int currentMinutes) {
        return previousMinutes == null ? currentMinutes > 0 : currentMinutes > previousMinutes;
    }

    private int syncUnlocks(PlatformLibraryPort library, PlatformAccountView account,
                            String platformGameId, long gameId) {
        List<PlatformUnlock> rawUnlocks = library.fetchUnlocks(account.externalId(), platformGameId);
        if (rawUnlocks.isEmpty()) {
            return 0;
        }
        Map<String, Long> achievementIds = achievementResolver.resolveAchievementIds(gameId, account.platform(),
                rawUnlocks.stream().map(PlatformUnlock::platformAchievementId).toList());

        List<Unlock> unlocks = rawUnlocks.stream()
                .filter(u -> achievementIds.containsKey(u.platformAchievementId()))
                .map(u -> new Unlock(achievementIds.get(u.platformAchievementId()), u.unlockedAt()))
                .toList();
        if (unlocks.isEmpty()) {
            return 0;
        }
        return unlockRecorder.record(new RecordUnlocksUseCase.Command(account.userId(), account.id(), unlocks));
    }
}
