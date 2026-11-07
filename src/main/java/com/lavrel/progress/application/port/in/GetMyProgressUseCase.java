package com.lavrel.progress.application.port.in;

import java.time.Instant;
import java.util.List;

/** The signed-in user's own totals and latest unlocks, for the home screen. Only published games count. */
public interface GetMyProgressUseCase {

    int DEFAULT_RECENT = 3;
    int MAX_RECENT = 20;

    MyStats stats(long userId);

    /** The latest unlocks, newest first; limit is kept between 1 and {@value #MAX_RECENT}. */
    List<RecentUnlock> recentUnlocks(long userId, int limit);

    /**
     * gamesOwned: published games in the user's linked accounts. perfectGames: owned games with every achievement
     * unlocked. averageCompletion: the average share of achievements unlocked over the owned games where at least one
     * is unlocked (Steam's definition), in percent with one decimal; null when there are none.
     */
    record MyStats(int achievementsUnlocked, Double averageCompletion, int perfectGames, int gamesOwned) {
    }

    /** globalPercent: share of the game's owners who unlocked it (rarity); null while nobody owns the game. */
    record RecentUnlock(long achievementId, String name, String iconUrl, boolean hidden, long gameId,
                        String gameTitle, String gameSlug, Double globalPercent, Instant unlockedAt) {
    }
}
