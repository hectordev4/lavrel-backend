package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.in.ImportPlatformGamesUseCase.ImportOutcome;
import com.lavrel.catalog.application.service.PlatformGameImporter;
import com.lavrel.shared.domain.Platform;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Steam's importer for account syncs: every safe game that has achievements, whatever its popularity.
 * Known apps and recent failures cost no Steam call; only real attempts count toward maxAttempts.
 * If Steam becomes unavailable the run stops and the rest is reported as deferred.
 */
@Service
public class ImportSteamGamesService implements PlatformGameImporter {

    private final SteamImportRun importRun;
    private final long delayMs;

    ImportSteamGamesService(SteamImportRun importRun, @Value("${app.catalog.import-delay-ms:300}") long delayMs) {
        this.importRun = importRun;
        this.delayMs = delayMs;
    }

    @Override
    public Platform platform() {
        return Platform.STEAM;
    }

    @Override
    public ImportOutcome importMissing(List<String> steamAppIds, int maxAttempts) {
        SteamImportRun.Result run = importRun.run(steamAppIds, maxAttempts, ImportMode.SYNC, delayMs);
        return new ImportOutcome(run.imported(), run.deferred());
    }
}
