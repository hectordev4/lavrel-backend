package com.lavrel.catalog.application.port.out;

import com.lavrel.shared.domain.Platform;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;

/** Remembers platform apps that could not be imported as a game, so they are not asked for again. */
public interface ImportFailureRepositoryPort {

    /**
     * @param ignoredReasons failures with exactly one of these reasons are not reported
     * @return the ids among platformGameIds that failed at or after {@code since}
     */
    Set<String> findFailedSince(Platform platform, Collection<String> platformGameIds, Instant since,
                                Collection<String> ignoredReasons);

    void recordFailure(Platform platform, String platformGameId, String reason, Instant at);
}
