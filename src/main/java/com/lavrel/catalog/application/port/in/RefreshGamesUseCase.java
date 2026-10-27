package com.lavrel.catalog.application.port.in;

import java.util.List;

/**
 * The manual sync with Steam: re-copies the details of games that are already published (tags, genres,
 * creators, features, ratings, title, description...), replacing what is stored. The games stay visible while
 * this runs, and each one is judged by the content policy again: a game that no longer passes is removed.
 * Tags and genres never change on their own, only through this call.
 */
public interface RefreshGamesUseCase {

    /**
     * @param steamAppIds     the games to refresh; when empty, the games not refreshed in olderThanDays (or never)
     * @param refreshTagNames also replace the stored tag names with Steam's current ones
     */
    RefreshResult refreshGames(List<String> steamAppIds, int olderThanDays, int limit, boolean refreshTagNames);

    record RefreshResult(int refreshed, int removed, int notInCatalog, int remaining, boolean stoppedEarly) {
    }
}
