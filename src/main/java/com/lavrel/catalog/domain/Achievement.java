package com.lavrel.catalog.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * APP. Our own copy of the Steam achievement list; a snapshot that changes only on a deliberate refresh.
 * Unique on (gameId, steamAchievementId).
 */
public record Achievement(
        Long id,
        long gameId,
        String steamAchievementId,
        String name,
        String description,
        String iconUrl,
        boolean hidden,
        Instant updatedAt) {

    public Achievement {
        Objects.requireNonNull(steamAchievementId, "steamAchievementId");
        Objects.requireNonNull(name, "name");
    }
}
