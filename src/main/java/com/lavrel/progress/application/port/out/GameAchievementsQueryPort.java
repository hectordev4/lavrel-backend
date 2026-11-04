package com.lavrel.progress.application.port.out;

import java.time.Instant;
import java.util.List;

/** Reads achievement (catalog) with user_achievement: the documented read-model exception. */
public interface GameAchievementsQueryPort {

    /** Every achievement of the game in Steam's order; unlockedAt filled for userId (null: none). */
    List<AchievementRow> findAchievements(long gameId, Long userId);

    record AchievementRow(long id, String name, String description, String iconUrl, boolean hidden,
                          Instant unlockedAt) {
    }
}
