package com.lavrel.catalog.application.port.out;

import com.lavrel.catalog.domain.Game;
import java.util.Optional;

public interface GameRepositoryPort {

    Optional<Game> findById(long id);

    Game save(Game game);
}
