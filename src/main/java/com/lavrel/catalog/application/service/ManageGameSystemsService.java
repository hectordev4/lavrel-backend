package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.ManageGameSystemsUseCase;
import com.lavrel.catalog.application.port.out.GameRepositoryPort;
import com.lavrel.catalog.application.port.out.GameSystemRepositoryPort;
import com.lavrel.catalog.application.port.out.GameSystemRepositoryPort.Source;
import com.lavrel.catalog.domain.GameNotFoundException;
import com.lavrel.catalog.domain.GameSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every change is logged, so there is a trace of what was set by hand. */
@Service
public class ManageGameSystemsService implements ManageGameSystemsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageGameSystemsService.class);

    private final GameRepositoryPort games;
    private final GameSystemRepositoryPort systems;

    public ManageGameSystemsService(GameRepositoryPort games, GameSystemRepositoryPort systems) {
        this.games = games;
        this.systems = systems;
    }

    @Override
    @Transactional
    public void add(long gameId, GameSystem system) {
        requireGame(gameId);
        systems.add(gameId, system, Source.MANUAL);
        log.info("Game {} marked as available on {}", gameId, system);
    }

    @Override
    @Transactional
    public void remove(long gameId, GameSystem system) {
        requireGame(gameId);
        if (systems.remove(gameId, system)) {
            log.info("Game {} no longer marked as available on {}", gameId, system);
        }
    }

    private void requireGame(long gameId) {
        if (games.findById(gameId).isEmpty()) {
            throw new GameNotFoundException(gameId);
        }
    }
}
