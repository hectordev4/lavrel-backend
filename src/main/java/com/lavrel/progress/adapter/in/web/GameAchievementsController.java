package com.lavrel.progress.adapter.in.web;

import com.lavrel.progress.application.port.in.GetGameAchievementsUseCase;
import com.lavrel.progress.application.port.in.GetGameAchievementsUseCase.AchievementItem;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/games/{gameId}/achievements: public. With a token, each achievement also says whether you unlocked it
 * (and when) and the percentage in your country.
 */
@RestController
public class GameAchievementsController {

    private final GetGameAchievementsUseCase achievements;

    public GameAchievementsController(GetGameAchievementsUseCase achievements) {
        this.achievements = achievements;
    }

    @GetMapping("/api/games/{gameId}/achievements")
    public AchievementsResponse list(@AuthenticationPrincipal AuthenticatedUser user,
                                     @PathVariable("gameId") @Positive long gameId) {
        List<AchievementItem> items = achievements.list(gameId, user == null ? null : user.userId());
        int unlocked = (int) items.stream().filter(a -> a.unlockedAt() != null).count();
        return new AchievementsResponse(gameId, items.size(), user == null ? null : unlocked,
                items.stream().map(AchievementResponse::from).toList());
    }

    /** unlocked is null for anonymous visitors. */
    public record AchievementsResponse(long gameId, int total, Integer unlocked,
                                       List<AchievementResponse> achievements) {
    }

    /** Percentages rounded to one decimal; unlocked/unlockedAt only mean something with a token. */
    public record AchievementResponse(long id, String name, String description, String iconUrl, boolean hidden,
                                      Double globalPercent, Double countryPercent, boolean unlocked,
                                      Instant unlockedAt) {

        static AchievementResponse from(AchievementItem a) {
            return new AchievementResponse(a.id(), a.name(), a.description(), a.iconUrl(), a.hidden(),
                    round(a.globalPercent()), round(a.countryPercent()), a.unlockedAt() != null, a.unlockedAt());
        }

        private static Double round(Double value) {
            return value == null ? null : Math.round(value * 10.0) / 10.0;
        }
    }
}
