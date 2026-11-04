package com.lavrel.library.application.port.out;

public interface GameOwnershipPort {

    /** True when one of the user's linked accounts owns the game and it is published in the catalog. */
    boolean owns(long userId, long gameId);
}
