package com.lavrel.catalog.application.service.steam;

import java.util.Set;

/**
 * How strict an import is. Account syncs and explicit app lists import every safe game that has achievements;
 * bulk seeding also drops games with too few reviews, so a walk through all of Steam stays useful.
 * ignoredFailureReasons: remembered failures that do not apply in this mode (a game skipped by bulk seeding
 * for its review count can still be imported when a user owns it).
 */
enum ImportMode {

    SYNC(false, Set.of(SteamGameScreening.TOO_FEW_REVIEWS)),
    BULK(true, Set.of());

    private final boolean popularityRequired;
    private final Set<String> ignoredFailureReasons;

    ImportMode(boolean popularityRequired, Set<String> ignoredFailureReasons) {
        this.popularityRequired = popularityRequired;
        this.ignoredFailureReasons = ignoredFailureReasons;
    }

    boolean popularityRequired() {
        return popularityRequired;
    }

    Set<String> ignoredFailureReasons() {
        return ignoredFailureReasons;
    }
}
