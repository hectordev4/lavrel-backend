package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.in.RefreshGamesUseCase;
import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.GameMetadataRepositoryPort;
import com.lavrel.catalog.domain.GameMapping;
import com.lavrel.shared.domain.Platform;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Picks the games to refresh (the given ones, or the stale ones) and has their Steam details copied again. */
@Service
public class RefreshGamesService implements RefreshGamesUseCase {

    private final GameMetadataRepositoryPort metadata;
    private final GameMappingRepositoryPort gameMappings;
    private final SteamGameReprocessor reprocessor;
    private final SteamTagNames tagNames;
    private final Clock clock;

    RefreshGamesService(GameMetadataRepositoryPort metadata, GameMappingRepositoryPort gameMappings,
                        SteamGameReprocessor reprocessor, SteamTagNames tagNames, Clock clock) {
        this.metadata = metadata;
        this.gameMappings = gameMappings;
        this.reprocessor = reprocessor;
        this.tagNames = tagNames;
        this.clock = clock;
    }

    @Override
    public RefreshResult refreshGames(List<String> steamAppIds, int olderThanDays, int limit,
                                      boolean refreshTagNames) {
        if (refreshTagNames) {
            tagNames.replaceAll();
        }
        Instant olderThan = clock.instant().minus(Duration.ofDays(olderThanDays));

        List<Long> gameIds = new ArrayList<>();
        int notInCatalog = 0;
        if (steamAppIds.isEmpty()) {
            gameIds.addAll(metadata.findStaleGameIds(olderThan, limit));
        } else {
            List<GameMapping> mappings = gameMappings.findByPlatformAndPlatformGameIds(Platform.STEAM, steamAppIds);
            mappings.forEach(m -> gameIds.add(m.gameId()));
            notInCatalog = steamAppIds.size() - mappings.size();
        }
        SteamGameReprocessor.Result done = reprocessor.reprocess(gameIds);
        return new RefreshResult(done.checked() - done.removed(), done.removed(), notInCatalog,
                metadata.countStale(olderThan), done.stoppedEarly());
    }
}
