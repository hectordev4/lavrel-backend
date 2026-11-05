package com.lavrel.library.application.service;

import com.lavrel.library.application.port.in.AutoSyncUseCase;
import com.lavrel.library.application.port.in.SyncAccountUseCase;
import com.lavrel.library.application.port.in.SyncAccountUseCase.SyncResult;
import com.lavrel.library.application.port.out.DueAccountsQueryPort;
import com.lavrel.library.domain.ProfilePrivateException;
import com.lavrel.shared.domain.PlatformUnavailableException;
import com.lavrel.user.application.port.in.MarkAccountSyncedUseCase;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Each round takes a small batch of the accounts waiting longest and syncs only what changed (syncChangedGames).
 * Only users who used the app in the last {@code app.auto-sync.active-days} days and kept automatic sync on are
 * included. An account that fails for its own reasons (private profile, an odd game) is marked as synced so it does not
 * block the queue; if the platform itself fails, the round stops and the rest waits for the next one.
 */
@Service
public class AutoSyncService implements AutoSyncUseCase {

    private static final Logger log = LoggerFactory.getLogger(AutoSyncService.class);

    private final DueAccountsQueryPort dueAccounts;
    private final SyncAccountUseCase sync;
    private final MarkAccountSyncedUseCase marker;
    private final Clock clock;
    private final Duration every;
    private final Duration activeWindow;
    private final int batchSize;
    private final long pauseMs;

    public AutoSyncService(DueAccountsQueryPort dueAccounts, SyncAccountUseCase sync, MarkAccountSyncedUseCase marker,
                           Clock clock,
                           @Value("${app.auto-sync.every:PT6H}") Duration every,
                           @Value("${app.auto-sync.active-days:30}") int activeDays,
                           @Value("${app.auto-sync.batch-size:20}") int batchSize,
                           @Value("${app.auto-sync.pause-ms:1000}") long pauseMs) {
        this.dueAccounts = dueAccounts;
        this.sync = sync;
        this.marker = marker;
        this.clock = clock;
        this.every = every;
        this.activeWindow = Duration.ofDays(activeDays);
        this.batchSize = batchSize;
        this.pauseMs = pauseMs;
    }

    @Override
    public AutoSyncReport runOnce() {
        Instant now = clock.instant();
        List<Long> due = dueAccounts.findDue(now.minus(every), now.minus(activeWindow), batchSize);
        int synced = 0;
        int failed = 0;
        int unlocks = 0;
        boolean stoppedEarly = false;
        for (Long accountId : due) {
            try {
                SyncResult result = sync.syncChangedGames(accountId);
                unlocks += result.unlocksRecorded();
                synced++;
            } catch (PlatformUnavailableException e) {
                log.warn("Automatic sync stopped: platform unavailable ({})", e.getMessage());
                stoppedEarly = true;
                break;
            } catch (ProfilePrivateException e) {
                marker.markSynced(accountId);
                failed++;
            } catch (RuntimeException e) {
                log.warn("Automatic sync of account {} failed: {}", accountId, e.toString());
                marker.markSynced(accountId);
                failed++;
            }
            pause();
        }
        if (!due.isEmpty()) {
            log.info("Automatic sync: {} accounts synced, {} failed, {} unlocks recorded{}", synced, failed, unlocks,
                    stoppedEarly ? " (stopped early)" : "");
        }
        return new AutoSyncReport(synced, failed, unlocks, stoppedEarly);
    }

    private void pause() {
        if (pauseMs <= 0) {
            return;
        }
        try {
            Thread.sleep(pauseMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
