package com.lavrel.progress.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * PLATFORM. One row per (userId, achievementId), never repeated.
 * sourceAccountId is the account that triggered the unlock (nullable).
 */
public record UserAchievement(
        long userId,
        long achievementId,
        Long sourceAccountId,
        Instant unlockedAt,
        Instant recordedAt) {

    public UserAchievement {
        Objects.requireNonNull(unlockedAt, "unlockedAt");
        Objects.requireNonNull(recordedAt, "recordedAt");
    }
}
