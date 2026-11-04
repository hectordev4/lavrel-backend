package com.lavrel.library.application.port.in;

import com.lavrel.library.domain.CollectionLimitReachedException;
import com.lavrel.library.domain.CollectionNameTakenException;
import com.lavrel.library.domain.CollectionNotFoundException;
import com.lavrel.library.domain.GameNotOwnedException;
import com.lavrel.library.domain.InvalidCollectionNameException;
import com.lavrel.library.domain.InvalidCollectionOrderException;
import java.util.List;
import java.util.UUID;

/**
 * Collections of the user's own games. Up to {@value #MAX_COLLECTIONS} per user, names unique per user (ignoring
 * case). A game that leaves the library (an account is unlinked) stays in its collections but is hidden, and shows
 * again if the user owns it again.
 */
public interface ManageCollectionsUseCase {

    int MAX_COLLECTIONS = 50;

    List<CollectionView> list(long userId);

    /**
     * @throws InvalidCollectionNameException  if the name breaks the rules
     * @throws CollectionNameTakenException    if the user already has one with that name
     * @throws CollectionLimitReachedException if the user has {@value #MAX_COLLECTIONS} already
     */
    CollectionView create(long userId, String name);

    /** @throws CollectionNotFoundException if it is not one of the user's collections */
    CollectionView rename(long userId, UUID collectionId, String name);

    /** Deletes the collection only; the games stay in the library. */
    void delete(long userId, UUID collectionId);

    /** @throws InvalidCollectionOrderException unless the list has every collection of the user exactly once */
    List<CollectionView> reorder(long userId, List<UUID> collectionIds);

    /** Idempotent. @throws GameNotOwnedException if the game is not in the user's library */
    void addGame(long userId, UUID collectionId, long gameId);

    /** Idempotent. */
    void removeGame(long userId, UUID collectionId, long gameId);

    /** gameCount: the games the library shows in it now (owned and published). */
    record CollectionView(UUID id, String name, int position, int gameCount) {
    }
}
