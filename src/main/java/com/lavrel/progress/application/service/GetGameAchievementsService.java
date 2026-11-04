package com.lavrel.progress.application.service;

import com.lavrel.catalog.application.port.in.CheckPublishedGameUseCase;
import com.lavrel.progress.application.port.in.GetGameAchievementsUseCase;
import com.lavrel.progress.application.port.out.AchievementStatsQueryPort;
import com.lavrel.progress.application.port.out.GameAchievementsQueryPort;
import com.lavrel.progress.domain.AchievementStats;
import com.lavrel.user.application.port.in.GetUserCountryUseCase;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Joins the list (with the user's unlocks) and the statistics, which use the same rules as GET /stats. */
@Service
@Transactional(readOnly = true)
public class GetGameAchievementsService implements GetGameAchievementsUseCase {

    private final CheckPublishedGameUseCase catalog;
    private final GameAchievementsQueryPort achievements;
    private final AchievementStatsQueryPort stats;
    private final GetUserCountryUseCase userCountry;
    private final int minCountrySample;

    public GetGameAchievementsService(CheckPublishedGameUseCase catalog, GameAchievementsQueryPort achievements,
                                      AchievementStatsQueryPort stats, GetUserCountryUseCase userCountry,
                                      @Value("${app.stats.min-country-sample:20}") int minCountrySample) {
        this.catalog = catalog;
        this.achievements = achievements;
        this.stats = stats;
        this.userCountry = userCountry;
        this.minCountrySample = minCountrySample;
    }

    @Override
    public List<AchievementItem> list(long gameId, Long userId) {
        catalog.requirePublished(gameId);
        Integer countryId = userId == null ? null : userCountry.getCountryId(userId);
        Map<Long, AchievementStats> byId = stats.findGameStats(gameId, countryId, minCountrySample).stream()
                .collect(Collectors.toMap(AchievementStats::achievementId, Function.identity()));
        return achievements.findAchievements(gameId, userId).stream().map(row -> {
            AchievementStats s = byId.get(row.id());
            return new AchievementItem(row.id(), row.name(), row.description(), row.iconUrl(), row.hidden(),
                    s == null ? null : s.globalPercent(), s == null ? null : s.countryPercent(), row.unlockedAt());
        }).toList();
    }
}
