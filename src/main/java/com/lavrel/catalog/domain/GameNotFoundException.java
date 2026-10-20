package com.lavrel.catalog.domain;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(long gameId) {
        super("Game not found: " + gameId);
    }
}
