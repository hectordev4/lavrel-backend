package com.lavrel.catalog.application.port.out;

import com.lavrel.catalog.domain.GameMetadata;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Stores the details of a game. Saving them is what publishes the game. */
public interface GameMetadataRepositoryPort {

    /** Replaces the game's details and marks it as published. */
    void save(long gameId, GameMetadata metadata);

    /** @return the details, or empty if the game is not published yet */
    Optional<GameMetadata> find(long gameId);

    /** Games that were never enriched: hidden until they are. */
    List<Long> findPendingGameIds(int limit);

    int countPending();

    /** Published games whose details were last copied before olderThan (or never): due for a refresh. */
    List<Long> findStaleGameIds(Instant olderThan, int limit);

    int countStale(Instant olderThan);

    Set<Long> findKnownTagIds(Collection<Long> tagIds);

    /**
     * @param overwrite false: only fills tags that have no real name yet (placeholders), existing names are
     *                  never touched; true: replaces every name (the manual sync with Steam)
     */
    void saveTagNames(Map<Long, String> names, boolean overwrite);
}
