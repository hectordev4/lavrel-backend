package com.lavrel.tracking.domain;

public class GameNotTrackedException extends RuntimeException {

    public GameNotTrackedException(long gameId) {
        super("Game " + gameId + " is not in your tracking list");
    }
}
