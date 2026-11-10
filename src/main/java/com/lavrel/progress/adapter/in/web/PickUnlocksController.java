package com.lavrel.progress.adapter.in.web;

import com.lavrel.progress.application.port.in.PickUnlocksUseCase;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.AchievementSort;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.GameSort;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.GamesPage;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.UnlockedAchievement;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The achievement picker for profile shelves (private):
 *  - GET /api/currentuser/achievements/games?q=&sort=recent|name&page=&size=
 *        games where I unlocked something, with how many
 *  - GET /api/currentuser/games/{gameId}/achievements/unlocked?sort=rarity|date
 *        my unlocked achievements in that game, rarest or newest first
 */
@RestController
public class PickUnlocksController {

    private final PickUnlocksUseCase picker;

    public PickUnlocksController(PickUnlocksUseCase picker) {
        this.picker = picker;
    }

    @GetMapping("/api/currentuser/achievements/games")
    public GamesPage games(@AuthenticationPrincipal AuthenticatedUser user,
                           @RequestParam(name = "q", required = false) @Size(max = 100) String q,
                           @RequestParam(name = "sort", defaultValue = "recent")
                           @Pattern(regexp = "(?i)recent|name") String sort,
                           @RequestParam(name = "page", defaultValue = "0") @Min(0) @Max(1000) int page,
                           @RequestParam(name = "size", defaultValue = "24")
                           @Min(1) @Max(PickUnlocksUseCase.MAX_PAGE_SIZE) int size) {
        return picker.gamesWithUnlocks(user.userId(), q, GameSort.valueOf(sort.toUpperCase(Locale.ROOT)), page, size);
    }

    @GetMapping("/api/currentuser/games/{gameId}/achievements/unlocked")
    public UnlockedResponse unlocked(@AuthenticationPrincipal AuthenticatedUser user,
                                     @PathVariable("gameId") @Positive long gameId,
                                     @RequestParam(name = "sort", defaultValue = "rarity")
                                     @Pattern(regexp = "(?i)rarity|date") String sort) {
        return new UnlockedResponse(gameId, picker.unlockedIn(user.userId(), gameId,
                AchievementSort.valueOf(sort.toUpperCase(Locale.ROOT))));
    }

    public record UnlockedResponse(long gameId, List<UnlockedAchievement> achievements) {
    }
}
