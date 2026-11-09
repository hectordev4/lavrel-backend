package com.lavrel.assistant.application.port.out;

import com.lavrel.assistant.domain.AchievementContext;
import java.util.Optional;

/** Reads achievement and game (catalog tables, the documented read-model exception). Published games only. */
public interface AchievementContextPort {

    Optional<AchievementContext> find(long gameId, long achievementId);

    /** The same, by the achievement alone (admin tools). */
    Optional<AchievementContext> findById(long achievementId);

    /** The title of a published game, or empty. */
    Optional<String> publishedGameTitle(long gameId);
}
