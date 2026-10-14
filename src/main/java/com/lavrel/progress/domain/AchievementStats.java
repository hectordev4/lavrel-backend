package com.lavrel.progress.domain;

/**
 * Not persisted: a query result. countryId and countryPercent are null when the user has no country
 * or the country sample is below the minimum.
 */
public record AchievementStats(
        long achievementId,
        double globalPercent,
        Integer countryId,
        Double countryPercent) {
}
