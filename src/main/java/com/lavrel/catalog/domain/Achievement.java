package com.lavrel.catalog.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * APP. Our own copy of the Steam achievement list; a snapshot that changes only on a deliberate refresh.
 * Unique on (gameId, steamAchievementId).
 * Texts longer than the columns (migration V18) are clipped here, so one odd achievement from Steam can never make a
 * whole sync fail. The Steam id is an identifier and is never clipped.
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

    public static final int MAX_NAME = 1024;
    public static final int MAX_DESCRIPTION = 8000;
    public static final int MAX_ICON_URL = 1024;

    public Achievement {
        Objects.requireNonNull(steamAchievementId, "steamAchievementId");
        Objects.requireNonNull(name, "name");
        name = clip(name, MAX_NAME);
        description = clip(description, MAX_DESCRIPTION);
        iconUrl = clip(iconUrl, MAX_ICON_URL);
    }

    /** At most max characters, never cutting an emoji or other two-part character in half. */
    static String clip(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        int end = Character.isHighSurrogate(value.charAt(max - 1)) ? max - 1 : max;
        return value.substring(0, end);
    }
}
