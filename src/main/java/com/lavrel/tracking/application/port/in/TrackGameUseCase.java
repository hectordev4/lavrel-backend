package com.lavrel.tracking.application.port.in;

import com.lavrel.catalog.domain.GameNotFoundException;
import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.TrackedGameCard;
import com.lavrel.tracking.domain.GameNotTrackedException;
import com.lavrel.tracking.domain.TrackingStatus;

/** Adding, changing and removing games in the user's tracking list. Only the user does this. */
public interface TrackGameUseCase {

    /**
     * Adds the game with that status, or changes the status if it is already tracked.
     * Any game in the catalog can be tracked, owned or not (games from a sync are in the catalog).
     *
     * @throws GameNotFoundException if the game is not in the catalog
     */
    TrackedGameCard track(long userId, long gameId, TrackingStatus status);

    /**
     * Removes the game from the list (and from quick access).
     *
     * @throws GameNotTrackedException if it was not tracked
     */
    void untrack(long userId, long gameId);
}
