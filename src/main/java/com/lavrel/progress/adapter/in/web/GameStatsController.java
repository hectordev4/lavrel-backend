package com.lavrel.progress.adapter.in.web;

import com.lavrel.progress.application.port.in.GetAchievementStatsUseCase;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unlock percentages for every achievement of a game: global, plus the percentage inside the
 * current user's country (null when the user has no country or the sample is too small).
 */
@RestController
@RequestMapping("/api/games")
public class GameStatsController {

    private final GetAchievementStatsUseCase getAchievementStats;

    public GameStatsController(GetAchievementStatsUseCase getAchievementStats) {
        this.getAchievementStats = getAchievementStats;
    }

    @GetMapping("/{gameId}/stats")
    public GameStatsResponse getGameStats(@AuthenticationPrincipal AuthenticatedUser user,
                                          @PathVariable("gameId") @Positive long gameId) {
        return new GameStatsResponse(gameId, getAchievementStats.getGameStats(gameId, user.userId()).stream()
                .map(s -> new AchievementStatsResponse(s.achievementId(), round(s.globalPercent()),
                        s.countryId(), s.countryPercent() == null ? null : round(s.countryPercent())))
                .toList());
    }

    private static double round(double percent) {
        return Math.round(percent * 10.0) / 10.0;
    }

    /** Wrapped in an object so fields (sample size, paging...) can be added later without breaking clients. */
    public record GameStatsResponse(long gameId, List<AchievementStatsResponse> achievements) {
    }

    public record AchievementStatsResponse(long achievementId, double globalPercent,
                                           Integer countryId, Double countryPercent) {
    }
}
