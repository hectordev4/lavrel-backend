package com.lavrel.tracking.application.port.in;

import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.TrackedGameCard;
import com.lavrel.tracking.domain.GameNotTrackedException;
import com.lavrel.tracking.domain.InvalidQuickAccessOrderException;
import com.lavrel.tracking.domain.QuickAccessFullException;
import java.util.List;

/** Quick access: up to 20 pinned games of the tracking list, in the user's order. */
public interface ManageQuickAccessUseCase {

    /**
     * Pins a tracked game at the end of quick access (nothing happens if it is pinned already).
     * @return quick access after the change
     * @throws GameNotTrackedException  if the game is not in the tracking list
     * @throws QuickAccessFullException if 20 games are pinned already
     */
    List<TrackedGameCard> pin(long userId, long gameId);

    /**
     * Takes a game out of quick access; it stays in the tracking list.
     * @throws GameNotTrackedException if the game is not in the tracking list
     */
    void unpin(long userId, long gameId);

    /**
     * @param gameIds every pinned game exactly once, in the new order
     * @return quick access after the change
     * @throws InvalidQuickAccessOrderException if the list is not exactly the pinned games
     */
    List<TrackedGameCard> reorder(long userId, List<Long> gameIds);
}
