package com.lavrel.library.application.service;

import com.lavrel.catalog.application.port.in.ResolveAchievementUseCase;
import com.lavrel.catalog.application.port.in.ResolveGameUseCase;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.lavrel.library.application.port.in.SyncAccountUseCase;
import com.lavrel.library.application.port.out.AccountGameRepositoryPort;
import com.lavrel.library.application.port.out.PlatformLibraryPort;
import com.lavrel.library.application.port.out.PlatformLibraryPort.PlatformGame;
import com.lavrel.library.application.port.out.PlatformLibraryPort.PlatformUnlock;
import com.lavrel.library.domain.AccountGame;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import com.lavrel.progress.application.port.in.RecordUnlocksUseCase;
import com.lavrel.progress.application.port.in.RecordUnlocksUseCase.Unlock;
import com.lavrel.shared.domain.Platform;
import com.lavrel.user.application.port.in.GetPlatformAccountUseCase;
import com.lavrel.user.application.port.in.GetPlatformAccountUseCase.PlatformAccountView;
import com.lavrel.user.domain.PlatformAccountNotFoundException;

/**
 * Orchestrates a sync. Platform data is translated to our IDs here, through other features'
 * inbound ports only. Network calls never run inside a database transaction.
 */
@Service
public class SyncAccountService implements SyncAccountUseCase {

    private final Map<Platform, PlatformLibraryPort> libraries = new EnumMap<>(Platform.class);
    private final GetPlatformAccountUseCase accounts;
    private final ResolveGameUseCase gameResolver;
    private final ResolveAchievementUseCase achievementResolver;
    private final RecordUnlocksUseCase unlockRecorder;
    private final AccountGameRepositoryPort accountGames;
    private final TransactionTemplate tx;
    private final Clock clock;

    public SyncAccountService(List<PlatformLibraryPort> ports, GetPlatformAccountUseCase accounts,
                              ResolveGameUseCase gameResolver, ResolveAchievementUseCase achievementResolver,
                              RecordUnlocksUseCase unlockRecorder, AccountGameRepositoryPort accountGames,
                              TransactionTemplate tx, Clock clock) {
        ports.forEach(p -> libraries.put(p.platform(), p));
        this.accounts = accounts;
        this.gameResolver = gameResolver;
        this.achievementResolver = achievementResolver;
        this.unlockRecorder = unlockRecorder;
        this.accountGames = accountGames;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public SyncResult sync(long userId, long platformAccountId) {
        PlatformAccountView account = accounts.getById(platformAccountId);
        if (account.userId() != userId) {
            // Do not reveal that the account exists for someone else.
            throw new PlatformAccountNotFoundException(platformAccountId);
        }
        PlatformLibraryPort library = libraries.get(account.platform());
        if (library == null) {
            throw new IllegalStateException("No library adapter for " + account.platform());
        }

        Instant now = clock.instant();
        List<PlatformGame> owned = library.fetchOwnedGames(account.externalId());
        Map<String, Long> gameIds = gameResolver.resolveGameIds(account.platform(),
                owned.stream().map(PlatformGame::platformGameId).toList());

        List<AccountGame> toSave = new ArrayList<>();
        int skipped = 0;
        int unlocksRecorded = 0;

        for (PlatformGame game : owned) {
            Long gameId = gameIds.get(game.platformGameId());
            if (gameId == null) {
                skipped++;           // unmapped game: skipped for now (a review queue could come later)
                continue;
            }
            toSave.add(new AccountGame(platformAccountId, gameId, game.playtimeMinutes(),
                    game.lastPlayedAt(), now));
            unlocksRecorded += syncUnlocks(library, account, game.platformGameId(), gameId);
        }

        tx.executeWithoutResult(status -> accountGames.saveAll(toSave));
        return new SyncResult(toSave.size(), skipped, unlocksRecorded);
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
