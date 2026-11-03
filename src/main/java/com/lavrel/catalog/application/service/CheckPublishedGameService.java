package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.CheckPublishedGameUseCase;
import com.lavrel.catalog.application.port.out.GameMetadataRepositoryPort;
import com.lavrel.catalog.domain.GameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CheckPublishedGameService implements CheckPublishedGameUseCase {

    private final GameMetadataRepositoryPort metadata;

    public CheckPublishedGameService(GameMetadataRepositoryPort metadata) {
        this.metadata = metadata;
    }

    /** A game is published once its details were stored (metadata.find only returns those). */
    @Override
    public void requirePublished(long gameId) {
        if (metadata.find(gameId).isEmpty()) {
            throw new GameNotFoundException(gameId);
        }
    }
}
