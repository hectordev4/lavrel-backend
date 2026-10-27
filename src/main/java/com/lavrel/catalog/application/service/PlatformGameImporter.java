package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.ImportPlatformGamesUseCase.ImportOutcome;
import com.lavrel.shared.domain.PlatformBound;
import java.util.List;

/**
 * One per platform whose store can feed the catalog. Today only Steam; an Xbox or PSN importer is added by
 * implementing this interface, without touching the sync.
 */
public interface PlatformGameImporter extends PlatformBound {

    ImportOutcome importMissing(List<String> platformGameIds, int maxAttempts);
}
