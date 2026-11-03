package com.lavrel.tracking.application.port.in;

import com.lavrel.tracking.domain.TrackingStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface GetTrackedGamesUseCase {

    /** status null = every status. */
    List<TrackedGameCard> list(long userId, TrackingStatus status, Sort sort);

    /** The pinned games, in the order the user chose. */
    List<TrackedGameCard> quickAccess(long userId);

    /**
     * RELEASE (default): newest store release first, games without a date last. TITLE: alphabetical.
     * UPDATED: most recently changed first.
     */
    enum Sort {
        RELEASE, TITLE, UPDATED
    }

    /**
     * What the tracking list shows per game: progress (unlocked of total) and, as a hint, the three easiest
     * achievements the user has not unlocked yet (highest share of owners who unlocked them).
     */
    record TrackedGameCard(long gameId, String title, String coverImageUrl, LocalDate releaseDate,
                           TrackingStatus status, Integer pinPosition, int unlocked, int total,
                           List<NextAchievement> nextAchievements, Instant addedAt, Instant updatedAt) {
    }

    /** globalPercent is null while nobody owns the game yet; hidden achievements should be shown blurred. */
    record NextAchievement(long achievementId, String name, String iconUrl, boolean hidden, Double globalPercent) {
    }
}
