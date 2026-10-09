package catalog.domain;

import shared.domain.Platform;
import java.util.Objects;

/**
 * MIXED. platformAchievementId comes from the platform; available is computed by our comparison.
 * Key: (achievementId, platform). platformAchievementId is null when the platform has no equivalent.
 */
public record AchievementMapping(
        long achievementId,
        Platform platform,
        String platformAchievementId,
        boolean available) {

    public AchievementMapping {
        Objects.requireNonNull(platform, "platform");
        if (available && platformAchievementId == null) {
            throw new IllegalArgumentException("platformAchievementId is required when available");
        }
    }

    public static AchievementMapping matched(long achievementId, Platform platform, String platformAchievementId) {
        return new AchievementMapping(achievementId, platform, platformAchievementId, true);
    }

    public static AchievementMapping unavailable(long achievementId, Platform platform) {
        return new AchievementMapping(achievementId, platform, null, false);
    }
}