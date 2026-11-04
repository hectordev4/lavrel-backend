package com.lavrel.library.domain;

/** Only games in the user's library (from a linked account) can go into a collection. */
public class GameNotOwnedException extends RuntimeException {

    public GameNotOwnedException(long gameId) {
        super("Game " + gameId + " is not in your library");
    }
}
