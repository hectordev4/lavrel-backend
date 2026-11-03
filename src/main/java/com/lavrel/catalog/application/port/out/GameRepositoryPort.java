package com.lavrel.catalog.application.port.out;

import com.lavrel.catalog.domain.Game;
import java.util.Optional;

public interface GameRepositoryPort {

    Optional<Game> findById(long id);

    Game save(Game game);

    /** Removes the game; its mappings, achievements, owners, unlocks and details go with it (database cascade). */
    void delete(long id);
}
