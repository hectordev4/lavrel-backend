package com.lavrel.catalog.adapter.in.web;

import com.lavrel.catalog.application.port.in.GetGameUseCase;
import com.lavrel.catalog.application.port.in.GetGameUseCase.GameDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the frontend shows for one game: description, Steam store page, release, platforms, reviews,
 * developers and publishers, tags, one boolean per store feature, and age ratings.
 * The use case's read model is the response shape.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GetGameUseCase getGame;

    public GameController(GetGameUseCase getGame) {
        this.getGame = getGame;
    }

    @GetMapping("/{gameId}")
    public GameDetails getGame(@PathVariable("gameId") long gameId) {
        return getGame.getGame(gameId);
    }
}
