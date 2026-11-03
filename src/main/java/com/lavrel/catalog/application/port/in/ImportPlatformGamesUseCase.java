package com.lavrel.catalog.application.port.in;

import com.lavrel.shared.domain.Platform;
import java.util.List;
import java.util.Map;

/**
 * Brings the games of a platform library into the catalog (used by account syncs). The caller does not need to
 * know which platform can feed the catalog: platforms without an importer simply import nothing.
 */
public interface ImportPlatformGamesUseCase {

    /**
     * Imports the platform games that are not in the catalog yet, in the given priority order. Only real
     * attempts count toward maxAttempts.
     */
    ImportOutcome importMissing(Platform platform, List<String> platformGameIds, int maxAttempts);

    /**
     * imported: platformGameId -> canonical game id, for the games imported by the call.
     * deferred: games that could still be imported but were not reached (attempt limit or platform trouble).
     */
    record ImportOutcome(Map<String, Long> imported, int deferred) {

        public ImportOutcome {
            imported = imported == null ? Map.of() : Map.copyOf(imported);
        }

        public static ImportOutcome none() {
            return new ImportOutcome(Map.of(), 0);
        }
    }
}
