package com.lavrel.profile.application.port.out;

import com.lavrel.profile.domain.Shelf;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** profile_shelf and profile_shelf_achievement (migration V23). Shelves come with their achievement ids. */
public interface ShelfRepositoryPort {

    /** In profile order. */
    List<Shelf> findAll(long userId);

    /** Only if it belongs to that user. */
    Optional<Shelf> find(long userId, UUID publicId);

    /** Inserts when id is null (returning it with its id), otherwise updates title, position and achievements. */
    Shelf save(Shelf shelf);

    /** Positions only. */
    void saveOrder(List<Shelf> shelves);

    void delete(long shelfId);
}
