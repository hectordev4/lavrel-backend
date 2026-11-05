package com.lavrel.profile.adapter.in.web;

import com.lavrel.profile.application.port.in.ManageShelvesUseCase;
import com.lavrel.profile.application.port.in.ManageShelvesUseCase.ShelfView;
import com.lavrel.profile.domain.Shelf;
import com.lavrel.profile.domain.ShelfType;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The shelves of my profile (up to 10, in my order):
 *  - GET    /api/currentuser/profile/shelves
 *  - POST   /api/currentuser/profile/shelves {"type": "COLLECTION", "title": "My cozy games", "collectionId": "..."}
 *           types: COLLECTION, RECENTLY_PLAYED, PLAYING, COMPLETED, ACHIEVEMENTS, STATS (201)
 *  - PATCH  /api/currentuser/profile/shelves/{id} {"title": "..."}         new title (blank: none)
 *  - DELETE /api/currentuser/profile/shelves/{id}
 *  - PUT    /api/currentuser/profile/shelves {"shelfIds": [...]}           new order, every shelf once
 *  - PUT    /api/currentuser/profile/shelves/{id}/achievements {"achievementIds": [...]}  up to 12 of my unlocks
 */
@RestController
@RequestMapping("/api/currentuser/profile/shelves")
public class CurrentUserShelvesController {

    private final ManageShelvesUseCase shelves;

    public CurrentUserShelvesController(ManageShelvesUseCase shelves) {
        this.shelves = shelves;
    }

    @GetMapping
    public ShelvesResponse list(@AuthenticationPrincipal AuthenticatedUser user) {
        return new ShelvesResponse(shelves.list(user.userId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShelfView add(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody AddRequest request) {
        return shelves.add(user.userId(), request.type(), request.title(), request.collectionId());
    }

    @PatchMapping("/{shelfId}")
    public ShelfView retitle(@AuthenticationPrincipal AuthenticatedUser user,
                             @PathVariable("shelfId") UUID shelfId, @Valid @RequestBody TitleRequest request) {
        return shelves.retitle(user.userId(), shelfId, request.title());
    }

    @DeleteMapping("/{shelfId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable("shelfId") UUID shelfId) {
        shelves.delete(user.userId(), shelfId);
    }

    @PutMapping
    public ShelvesResponse reorder(@AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody ReorderRequest request) {
        return new ShelvesResponse(shelves.reorder(user.userId(), request.shelfIds()));
    }

    @PutMapping("/{shelfId}/achievements")
    public ShelfView showAchievements(@AuthenticationPrincipal AuthenticatedUser user,
                                      @PathVariable("shelfId") UUID shelfId,
                                      @Valid @RequestBody AchievementsRequest request) {
        return shelves.showAchievements(user.userId(), shelfId, request.achievementIds());
    }

    public record AddRequest(@NotNull ShelfType type, @Size(max = 200) String title, UUID collectionId) {
    }

    public record TitleRequest(@Size(max = 200) String title) {
    }

    public record ReorderRequest(@NotNull @Size(max = ManageShelvesUseCase.MAX_SHELVES) List<@NotNull UUID> shelfIds) {
    }

    public record AchievementsRequest(@NotNull @Size(max = Shelf.MAX_ACHIEVEMENTS)
                                      List<@NotNull @Positive Long> achievementIds) {
    }

    public record ShelvesResponse(List<ShelfView> shelves) {
    }
}
