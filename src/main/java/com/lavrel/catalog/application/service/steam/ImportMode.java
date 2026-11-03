package com.lavrel.catalog.application.service.steam;

import java.util.Set;

/**
 * How strict an import is.
 *  - SYNC: account syncs and explicit app lists. Every safe game that has achievements, whatever its popularity.
 *  - BULK: the walk through all of Steam. Also drops games with too few reviews, so the walk stays useful.
 *  - EXEMPT: content-policy exceptions chosen by an admin. The content policy is skipped; everything else
 *    (a real game, with achievements and a store page) still applies.
 * ignoredFailureReasons: remembered failures that do not apply in this mode (a game skipped by bulk seeding
 * for its review count can still be imported when a user owns it).
 */
enum ImportMode {

    SYNC(false, true, Set.of(SteamGameScreening.TOO_FEW_REVIEWS)),
    BULK(true, true, Set.of()),
    EXEMPT(false, false, Set.of());

    private final boolean popularityRequired;
    private final boolean contentPolicyApplied;
    private final Set<String> ignoredFailureReasons;

    ImportMode(boolean popularityRequired, boolean contentPolicyApplied, Set<String> ignoredFailureReasons) {
        this.popularityRequired = popularityRequired;
        this.contentPolicyApplied = contentPolicyApplied;
        this.ignoredFailureReasons = ignoredFailureReasons;
    }

    boolean popularityRequired() {
        return popularityRequired;
    }

    boolean contentPolicyApplied() {
        return contentPolicyApplied;
    }

    Set<String> ignoredFailureReasons() {
        return ignoredFailureReasons;
    }
}
