package com.lavrel.tracking.adapter.in.web;

import com.lavrel.tracking.adapter.in.web.TrackedGamesController.CardResponse;
import com.lavrel.tracking.adapter.in.web.TrackedGamesController.TrackedGamesResponse;
import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase;
import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.TrackedGameCard;
import com.lavrel.tracking.application.port.in.ManageQuickAccessUseCase;
import com.lavrel.tracking.domain.QuickAccess;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quick access: up to 20 pinned games of the tracking list, in the user's order.
 *  - GET    /api/currentuser/tracked/quick-access                      the pinned games, in order
 *  - PUT    /api/currentuser/tracked/{gameId}/pin                       pin at the end (409 quick_access_full)
 *  - DELETE /api/currentuser/tracked/{gameId}/pin                       unpin (stays in the tracking list)
 *  - PUT    /api/currentuser/tracked/quick-access {"gameIds": [7, 3]}   new order (every pinned game once)
 */
@RestController
@RequestMapping("/api/currentuser/tracked")
public class QuickAccessController {

    private final GetTrackedGamesUseCase getTrackedGames;
    private final ManageQuickAccessUseCase quickAccess;

    public QuickAccessController(GetTrackedGamesUseCase getTrackedGames, ManageQuickAccessUseCase quickAccess) {
        this.getTrackedGames = getTrackedGames;
        this.quickAccess = quickAccess;
    }

    @GetMapping("/quick-access")
    public TrackedGamesResponse list(@AuthenticationPrincipal AuthenticatedUser user) {
        return response(getTrackedGames.quickAccess(user.userId()));
    }

    @PutMapping("/{gameId}/pin")
    public TrackedGamesResponse pin(@AuthenticationPrincipal AuthenticatedUser user,
                                    @PathVariable("gameId") @Positive long gameId) {
        return response(quickAccess.pin(user.userId(), gameId));
    }

    @DeleteMapping("/{gameId}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpin(@AuthenticationPrincipal AuthenticatedUser user,
                      @PathVariable("gameId") @Positive long gameId) {
        quickAccess.unpin(user.userId(), gameId);
    }

    @PutMapping("/quick-access")
    public TrackedGamesResponse reorder(@AuthenticationPrincipal AuthenticatedUser user,
                                        @Valid @RequestBody ReorderRequest request) {
        return response(quickAccess.reorder(user.userId(), request.gameIds()));
    }

    public record ReorderRequest(@NotNull @Size(max = QuickAccess.MAX_PINNED) List<@NotNull @Positive Long> gameIds) {
    }

    private static TrackedGamesResponse response(List<TrackedGameCard> cards) {
        return new TrackedGamesResponse(cards.stream().map(CardResponse::from).toList());
    }
}
