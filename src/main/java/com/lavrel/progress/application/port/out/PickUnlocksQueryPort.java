package com.lavrel.progress.application.port.out;

import com.lavrel.progress.application.port.in.PickUnlocksUseCase.AchievementSort;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.GameSort;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.GameWithUnlocks;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.UnlockedAchievement;
import java.util.List;

/** Reads unlocks, achievements and games (the documented read-model exception). Published games only. */
public interface PickUnlocksQueryPort {

    /** One row more than limit may come back, so the caller can tell whether there is a next page. */
    List<GameWithUnlocks> gamesWithUnlocks(long userId, String query, GameSort sort, int limit, long offset);

    List<UnlockedAchievement> unlockedIn(long userId, long gameId, AchievementSort sort);
}
