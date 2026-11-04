package com.lavrel.library.adapter.in.web;

import com.lavrel.library.application.port.in.ManageCollectionsUseCase;
import com.lavrel.library.application.port.in.ManageCollectionsUseCase.CollectionView;
import com.lavrel.library.domain.CollectionNames;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
 * Collections of the user's own games:
 *  - GET    /api/currentuser/collections                              list, in order, with game counts
 *  - POST   /api/currentuser/collections {"name": "Cozy games"}       create (201)
 *  - PATCH  /api/currentuser/collections/{id} {"name": "..."}         rename
 *  - DELETE /api/currentuser/collections/{id}                         delete (the games stay in the library)
 *  - PUT    /api/currentuser/collections {"collectionIds": [...]}     new order, every collection once
 *  - PUT    /api/currentuser/collections/{id}/games/{gameId}          add an owned game (204)
 *  - DELETE /api/currentuser/collections/{id}/games/{gameId}          remove it (204)
 * Browse one with GET /api/currentuser/games?collection={id}.
 */
@RestController
@RequestMapping("/api/currentuser/collections")
public class CollectionsController {

    private final ManageCollectionsUseCase collections;

    public CollectionsController(ManageCollectionsUseCase collections) {
        this.collections = collections;
    }

    @GetMapping
    public CollectionsResponse list(@AuthenticationPrincipal AuthenticatedUser user) {
        return response(collections.list(user.userId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody NameRequest request) {
        return CollectionResponse.from(collections.create(user.userId(), request.name()));
    }

    @PatchMapping("/{collectionId}")
    public CollectionResponse rename(@AuthenticationPrincipal AuthenticatedUser user,
                                     @PathVariable("collectionId") UUID collectionId,
                                     @Valid @RequestBody NameRequest request) {
        return CollectionResponse.from(collections.rename(user.userId(), collectionId, request.name()));
    }

    @DeleteMapping("/{collectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user,
                       @PathVariable("collectionId") UUID collectionId) {
        collections.delete(user.userId(), collectionId);
    }

    @PutMapping
    public CollectionsResponse reorder(@AuthenticationPrincipal AuthenticatedUser user,
                                       @Valid @RequestBody ReorderRequest request) {
        return response(collections.reorder(user.userId(), request.collectionIds()));
    }

    @PutMapping("/{collectionId}/games/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addGame(@AuthenticationPrincipal AuthenticatedUser user,
                        @PathVariable("collectionId") UUID collectionId,
                        @PathVariable("gameId") @Positive long gameId) {
        collections.addGame(user.userId(), collectionId, gameId);
    }

    @DeleteMapping("/{collectionId}/games/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeGame(@AuthenticationPrincipal AuthenticatedUser user,
                           @PathVariable("collectionId") UUID collectionId,
                           @PathVariable("gameId") @Positive long gameId) {
        collections.removeGame(user.userId(), collectionId, gameId);
    }

    private static CollectionsResponse response(List<CollectionView> views) {
        return new CollectionsResponse(views.stream().map(CollectionResponse::from).toList());
    }

    /** The full rules (length, characters) are in CollectionNames. */
    public record NameRequest(@NotBlank @Size(max = CollectionNames.MAX_LENGTH * 2) String name) {
    }

    public record ReorderRequest(@NotNull @Size(max = ManageCollectionsUseCase.MAX_COLLECTIONS)
                                 List<@NotNull UUID> collectionIds) {
    }

    public record CollectionsResponse(List<CollectionResponse> collections) {
    }

    /** id is the collection's public id (a UUID); gameCount is what the library shows in it now. */
    public record CollectionResponse(UUID id, String name, int position, int gameCount) {

        static CollectionResponse from(CollectionView v) {
            return new CollectionResponse(v.id(), v.name(), v.position(), v.gameCount());
        }
    }
}
