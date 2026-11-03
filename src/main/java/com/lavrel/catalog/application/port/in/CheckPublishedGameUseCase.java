package com.lavrel.catalog.application.port.in;

import com.lavrel.catalog.domain.GameNotFoundException;

/** For other features (tracking...) that may only refer to games visible in the catalog. */
public interface CheckPublishedGameUseCase {

    /** @throws GameNotFoundException if the game does not exist or is not published (no details yet) */
    void requirePublished(long gameId);
}
