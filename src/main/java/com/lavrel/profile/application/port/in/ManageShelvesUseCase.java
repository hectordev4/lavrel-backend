package com.lavrel.profile.application.port.in;

import com.lavrel.profile.domain.InvalidShelfException;
import com.lavrel.profile.domain.ShelfLimitReachedException;
import com.lavrel.profile.domain.ShelfNotFoundException;
import com.lavrel.profile.domain.ShelfType;
import java.util.List;
import java.util.UUID;

/** The shelves of the user's own profile: up to {@value #MAX_SHELVES}, in the user's order. */
public interface ManageShelvesUseCase {

    int MAX_SHELVES = 10;

    List<ShelfView> list(long userId);

    /**
     * @param collectionId required for COLLECTION shelves (one of the user's collections), otherwise null
     * @throws InvalidShelfException      for a wrong combination, a repeated one-only shelf, or a collection that is not
     *                                    the user's
     * @throws ShelfLimitReachedException with {@value #MAX_SHELVES} shelves already
     */
    ShelfView add(long userId, ShelfType type, String title, UUID collectionId);

    /** @throws ShelfNotFoundException if it is not one of the user's shelves */
    ShelfView retitle(long userId, UUID shelfId, String title);

    void delete(long userId, UUID shelfId);

    List<ShelfView> reorder(long userId, List<UUID> shelfIds);

    /** Achievement shelves only; every achievement must be one the user unlocked. Up to 12, in order. */
    ShelfView showAchievements(long userId, UUID shelfId, List<Long> achievementIds);

    record ShelfView(UUID id, ShelfType type, String title, int position, UUID collectionId,
                     List<Long> achievementIds) {
    }
}
