package progress.application.port.in;

import java.util.List;
import progress.domain.AchievementStats;

public interface GetAchievementStatsUseCase {

    /**
     * Global percentage for every achievement of the game, plus the country percentage
     * of the user's country. Country fields are null if the user has no country
     * or the country sample is too small.
     */
    List<AchievementStats> getGameStats(long gameId, long userId);
}
