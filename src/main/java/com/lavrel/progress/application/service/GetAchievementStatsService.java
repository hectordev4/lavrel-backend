package com.lavrel.progress.application.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lavrel.progress.application.port.in.GetAchievementStatsUseCase;
import com.lavrel.progress.application.port.out.AchievementStatsQueryPort;
import com.lavrel.progress.domain.AchievementStats;
import com.lavrel.user.application.port.in.GetUserCountryUseCase;

@Service
@Transactional(readOnly = true)
public class GetAchievementStatsService implements GetAchievementStatsUseCase {

    private final AchievementStatsQueryPort statsQuery;
    private final GetUserCountryUseCase userCountry;
    private final int minCountrySample;

    public GetAchievementStatsService(AchievementStatsQueryPort statsQuery, GetUserCountryUseCase userCountry,
                                      @Value("${app.stats.min-country-sample:20}") int minCountrySample) {
        this.statsQuery = statsQuery;
        this.userCountry = userCountry;
        this.minCountrySample = minCountrySample;
    }

    @Override
    public List<AchievementStats> getGameStats(long gameId, long userId) {
        Integer countryId = userCountry.getCountryId(userId);   // null: user has no country
        return statsQuery.findGameStats(gameId, countryId, minCountrySample);
    }
}
