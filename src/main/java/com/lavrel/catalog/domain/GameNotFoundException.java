package com.lavrel.catalog.domain;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(long gameId) {
        super("Game not found: " + gameId);
    }

    /** For a page address that matches no game. */
    public GameNotFoundException(String slug) {
        super("No game at /games/" + slug);
    }
}
