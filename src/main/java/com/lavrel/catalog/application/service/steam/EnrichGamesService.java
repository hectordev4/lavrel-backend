package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.in.EnrichGamesUseCase;
import com.lavrel.catalog.application.port.out.GameMetadataRepositoryPort;
import org.springframework.stereotype.Service;

/** Picks the games that never got details and has them checked and published (or removed). */
@Service
public class EnrichGamesService implements EnrichGamesUseCase {

    private final GameMetadataRepositoryPort metadata;
    private final SteamGameReprocessor reprocessor;

    EnrichGamesService(GameMetadataRepositoryPort metadata, SteamGameReprocessor reprocessor) {
        this.metadata = metadata;
        this.reprocessor = reprocessor;
    }

    @Override
    public EnrichResult enrichPendingGames(int maxGames) {
        SteamGameReprocessor.Result done = reprocessor.reprocess(metadata.findPendingGameIds(maxGames));
        return new EnrichResult(done.checked(), done.removed(), metadata.countPending(), done.stoppedEarly());
    }
}
