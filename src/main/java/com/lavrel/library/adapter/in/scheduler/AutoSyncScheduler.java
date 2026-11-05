package com.lavrel.library.adapter.in.scheduler;

import com.lavrel.library.application.port.in.AutoSyncUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Starts a round of the automatic sync every app.auto-sync.check-every (default 30 minutes; each account is synced at
 * most every app.auto-sync.every, default 6 hours). It only runs while the app is awake, so on Render's free plan it
 * relies on the keep-awake monitor. Off with app.auto-sync.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "app.auto-sync.enabled", havingValue = "true", matchIfMissing = true)
public class AutoSyncScheduler {

    private final AutoSyncUseCase autoSync;

    public AutoSyncScheduler(AutoSyncUseCase autoSync) {
        this.autoSync = autoSync;
    }

    @Scheduled(initialDelayString = "${app.auto-sync.first-run-after:PT10M}",
               fixedDelayString = "${app.auto-sync.check-every:PT30M}")
    public void run() {
        autoSync.runOnce();
    }
}
