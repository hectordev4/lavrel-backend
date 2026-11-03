package com.lavrel.catalog.domain;

import com.lavrel.shared.domain.Platform;

/** The game exists in the catalog but is not linked to that platform's version of it. */
public class GameNotOnPlatformException extends RuntimeException {

    public GameNotOnPlatformException(long gameId, Platform platform) {
        super("Game " + gameId + " is not linked to " + platform);
    }
}
