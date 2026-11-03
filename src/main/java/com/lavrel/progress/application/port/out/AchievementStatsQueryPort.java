package com.lavrel.progress.application.port.out;

import java.util.List;
import com.lavrel.progress.domain.AchievementStats;

/** Read-only query port: the one place allowed to join across feature tables. */
public interface AchievementStatsQueryPort {

    /**
     * @param countryId            null for global stats only
     * @param minCountrySampleSize country percentages below this many players come back null
     */
    List<AchievementStats> findGameStats(long gameId, Integer countryId, int minCountrySampleSize);
}
