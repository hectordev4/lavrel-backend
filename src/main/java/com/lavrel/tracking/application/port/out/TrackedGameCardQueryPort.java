package com.lavrel.tracking.application.port.out;

import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.Sort;
import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.TrackedGameCard;
import com.lavrel.tracking.domain.TrackingStatus;
import java.util.List;

/**
 * Read-only query that builds the cards shown in the tracking list. Like the stats query, it deliberately reads
 * tables of other features (game, achievement, user_achievement, account_game): a documented exception to the
 * "talk through inbound ports" rule, because a read model joins data anyway.
 */
public interface TrackedGameCardQueryPort {

    /** status null = every status. */
    List<TrackedGameCard> findCards(long userId, TrackingStatus status, Sort sort);

    /** Quick access: only pinned games, in pin order. */
    List<TrackedGameCard> findPinnedCards(long userId);

    /** One card, or empty if the game is not tracked by the user. */
    List<TrackedGameCard> findCard(long userId, long gameId);
}
