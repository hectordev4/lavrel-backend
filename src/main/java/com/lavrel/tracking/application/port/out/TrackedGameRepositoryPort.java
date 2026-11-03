package com.lavrel.tracking.application.port.out;

import com.lavrel.tracking.domain.TrackedGame;
import java.util.List;
import java.util.Optional;

public interface TrackedGameRepositoryPort {

    Optional<TrackedGame> find(long userId, long gameId);

    /** The user's pinned games, in quick-access order. */
    List<TrackedGame> findPinned(long userId);

    /** Inserts or updates (the key is userId + gameId). */
    void save(TrackedGame trackedGame);

    /** Saves several entries in one go (used when quick-access positions shift). */
    void saveAll(List<TrackedGame> trackedGames);

    /** @return false if it was not tracked */
    boolean delete(long userId, long gameId);
}
