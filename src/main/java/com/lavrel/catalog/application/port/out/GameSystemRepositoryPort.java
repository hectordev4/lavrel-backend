package com.lavrel.catalog.application.port.out;

import com.lavrel.catalog.domain.GameSystem;
import java.util.Set;

/** game_system (migration V20). source says where the information came from: IMPORTED or MANUAL. */
public interface GameSystemRepositoryPort {

    Set<GameSystem> findByGame(long gameId);

    /** Idempotent: an existing entry keeps its source. */
    void add(long gameId, GameSystem system, Source source);

    /** @return false if it was not there */
    boolean remove(long gameId, GameSystem system);

    enum Source {
        IMPORTED, MANUAL
    }
}
