package com.lavrel.tracking.adapter.in.web;

import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase;
import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.NextAchievement;
import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.Sort;
import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.TrackedGameCard;
import com.lavrel.tracking.application.port.in.TrackGameUseCase;
import com.lavrel.tracking.domain.TrackingStatus;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The current user's tracking list. Games are only added here by the user (a sync never does it).
 *  - GET    /api/currentuser/tracked?status=playing&sort=release   status optional; sort release (default),
 *                                                                  title or updated
 *  - PUT    /api/currentuser/tracked/{gameId}  {"status": "PLAYING"} add, or change the status
 *  - DELETE /api/currentuser/tracked/{gameId}                        remove
 */
@RestController
@RequestMapping("/api/currentuser/tracked")
public class TrackedGamesController {

    private final TrackGameUseCase trackGame;
    private final GetTrackedGamesUseCase getTrackedGames;

    public TrackedGamesController(TrackGameUseCase trackGame, GetTrackedGamesUseCase getTrackedGames) {
        this.trackGame = trackGame;
        this.getTrackedGames = getTrackedGames;
    }

    @GetMapping
    public TrackedGamesResponse list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(name = "status", required = false)
            @Pattern(regexp = "(?i)playing|backlog|completed|dropped") String status,
            @RequestParam(name = "sort", defaultValue = "release")
            @Pattern(regexp = "(?i)release|title|updated") String sort) {
        TrackingStatus statusFilter = status == null ? null : TrackingStatus.valueOf(status.toUpperCase(Locale.ROOT));
        return new TrackedGamesResponse(getTrackedGames.list(user.userId(), statusFilter,
                Sort.valueOf(sort.toUpperCase(Locale.ROOT))).stream().map(CardResponse::from).toList());
    }

    @PutMapping("/{gameId}")
    public CardResponse track(@AuthenticationPrincipal AuthenticatedUser user,
                              @PathVariable("gameId") @Positive long gameId,
                              @Valid @RequestBody TrackRequest request) {
        return CardResponse.from(trackGame.track(user.userId(), gameId, request.status()));
    }

    @DeleteMapping("/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void untrack(@AuthenticationPrincipal AuthenticatedUser user,
                        @PathVariable("gameId") @Positive long gameId) {
        trackGame.untrack(user.userId(), gameId);
    }

    /** status: PLAYING, BACKLOG, COMPLETED or DROPPED. */
    public record TrackRequest(@NotNull TrackingStatus status) {
    }

    public record TrackedGamesResponse(List<CardResponse> games) {
    }

    /** slug is the page address (/games/{slug}); pinPosition is null when not in quick access. */
    public record CardResponse(long gameId, String title, String slug, String coverImageUrl, LocalDate releaseDate,
                               TrackingStatus status, Integer pinPosition, int unlocked, int total,
                               List<NextAchievementResponse> nextAchievements, Instant addedAt, Instant updatedAt) {

        static CardResponse from(TrackedGameCard c) {
            return new CardResponse(c.gameId(), c.title(), c.slug(), c.coverImageUrl(), c.releaseDate(),
                    c.status(), c.pinPosition(), c.unlocked(), c.total(),
                    c.nextAchievements().stream().map(NextAchievementResponse::from).toList(),
                    c.addedAt(), c.updatedAt());
        }
    }

    public record NextAchievementResponse(long achievementId, String name, String iconUrl, boolean hidden,
                                          Double globalPercent) {

        static NextAchievementResponse from(NextAchievement a) {
            return new NextAchievementResponse(a.achievementId(), a.name(), a.iconUrl(), a.hidden(),
                    a.globalPercent() == null ? null : Math.round(a.globalPercent() * 10.0) / 10.0);
        }
    }
}
