package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.ImportPlatformGamesUseCase;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformRegistry;
import java.util.List;
import org.springframework.stereotype.Service;

/** Routes an import to the importer of the platform; platforms without one import nothing. */
@Service
public class ImportPlatformGamesService implements ImportPlatformGamesUseCase {

    private final PlatformRegistry<PlatformGameImporter> importers;

    public ImportPlatformGamesService(List<PlatformGameImporter> importers) {
        this.importers = new PlatformRegistry<>(importers, "catalog import");
    }

    @Override
    public ImportOutcome importMissing(Platform platform, List<String> platformGameIds, int maxAttempts) {
        if (platformGameIds.isEmpty()) {
            return ImportOutcome.none();
        }
        return importers.find(platform)
                .map(importer -> importer.importMissing(platformGameIds, maxAttempts))
                .orElseGet(ImportOutcome::none);
    }
}
