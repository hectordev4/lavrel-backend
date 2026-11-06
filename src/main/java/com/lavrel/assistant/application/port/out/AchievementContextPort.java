package com.lavrel.assistant.application.port.out;

import com.lavrel.assistant.domain.AchievementContext;
import java.util.Optional;

/** Reads achievement and game (catalog tables, the documented read-model exception). Published games only. */
public interface AchievementContextPort {

    Optional<AchievementContext> find(long gameId, long achievementId);
}
