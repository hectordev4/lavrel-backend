package com.lavrel.catalog.adapter.in.web;

import com.lavrel.catalog.application.port.in.GetGameUseCase;
import com.lavrel.shared.web.PublicCache;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the frontend shows for one game: description, Steam store page, release, platforms, reviews,
 * developers and publishers, tags, one boolean per store feature, and age ratings.
 * The use case's read model is mapped to GameResponse, the API's own shape.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GetGameUseCase getGame;

    public GameController(GetGameUseCase getGame) {
        this.getGame = getGame;
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(@PathVariable("gameId") @Positive long gameId) {
        return ResponseEntity.ok().cacheControl(PublicCache.GAME).body(GameResponse.from(getGame.getGame(gameId)));
    }

    /** The page address the frontend uses: /games/{slug}, e.g. /games/dark-souls-iii. */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<GameResponse> getGameBySlug(
            @PathVariable("slug") @Pattern(regexp = "[a-z0-9-]{1,200}") String slug) {
        return ResponseEntity.ok().cacheControl(PublicCache.GAME)
                .body(GameResponse.from(getGame.getGameBySlug(slug)));
    }
}
