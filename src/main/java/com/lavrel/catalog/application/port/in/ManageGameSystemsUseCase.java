package com.lavrel.catalog.application.port.in;

import com.lavrel.catalog.domain.GameNotFoundException;
import com.lavrel.catalog.domain.GameSystem;

/**
 * Admin: says which systems a game is available on. PC comes automatically from Steam; Xbox and PlayStation are set
 * here until their adapters exist. Removing PC from a Steam game lasts only until its next refresh.
 */
public interface ManageGameSystemsUseCase {

    /** Idempotent. @throws GameNotFoundException if the game does not exist */
    void add(long gameId, GameSystem system);

    /** Idempotent. @throws GameNotFoundException if the game does not exist */
    void remove(long gameId, GameSystem system);
}
