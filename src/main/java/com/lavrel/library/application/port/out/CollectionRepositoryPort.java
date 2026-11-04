package com.lavrel.library.application.port.out;

import com.lavrel.library.domain.Collection;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CollectionRepositoryPort {

    /** The user's collections, in their order. */
    List<Collection> findAll(long userId);

    /** Only if it belongs to that user. */
    Optional<Collection> find(long userId, UUID publicId);

    /** Ignoring case; exceptPublicId (nullable) is the collection being renamed. */
    boolean nameTaken(long userId, String name, UUID exceptPublicId);

    /** Inserts when id is null (returning it with its id), otherwise updates name and position. */
    Collection save(Collection collection);

    void saveAll(List<Collection> collections);

    void delete(long collectionId);

    /** @return false if it was already there */
    boolean addGame(long collectionId, long gameId, Instant at);

    /** @return false if it was not there */
    boolean removeGame(long collectionId, long gameId);

    /** Per collection id: how many of its games the user owns now and are published (what the library shows). */
    Map<Long, Integer> visibleGameCounts(long userId);
}
