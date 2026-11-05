package com.lavrel.profile.application.port.out;

import com.lavrel.profile.application.port.in.ViewProfilesUseCase.AchievementItem;
import com.lavrel.profile.application.port.in.ViewProfilesUseCase.GameItem;
import com.lavrel.profile.application.port.in.ViewProfilesUseCase.Stats;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Fills shelves with content. Reads the tables of the library, tracking, progress and catalog features (the
 * documented read-model exception). Only owned, published games and achievements the user still has are returned.
 */
public interface ShelfContentQueryPort {

    /** The collection's name and its visible games, newest first; empty if the collection no longer exists. */
    Optional<CollectionContent> collection(long userId, UUID collectionId, int limit);

    List<GameItem> recentlyPlayed(long userId, int limit);

    List<GameItem> playing(long userId, int limit);

    List<GameItem> completed(long userId, int limit);

    /** The picked achievements the user has unlocked, in the given order. */
    List<AchievementItem> achievements(long userId, List<Long> achievementIds);

    Stats stats(long userId);

    /** Which of these achievements the user has unlocked. */
    Set<Long> unlockedAmong(long userId, Collection<Long> achievementIds);

    record CollectionContent(String name, List<GameItem> games) {
    }
}
