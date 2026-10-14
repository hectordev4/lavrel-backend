package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.ImportSteamCatalogUseCase;
import com.lavrel.catalog.application.port.out.AchievementRepositoryPort;
import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.GameRepositoryPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformAchievement;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformGameInfo;
import com.lavrel.catalog.domain.Achievement;
import com.lavrel.catalog.domain.Game;
import com.lavrel.catalog.domain.GameMapping;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import com.lavrel.shared.domain.Platform;

/**
 * Network calls happen first; the database writes then run in one short transaction.
 * An already-imported game keeps its Game row (snapshot) and only refreshes its achievements.
 */
@Service
public class ImportSteamCatalogService implements ImportSteamCatalogUseCase {

    private final PlatformCatalogPort steam;
    private final GameRepositoryPort games;
    private final GameMappingRepositoryPort gameMappings;
    private final AchievementRepositoryPort achievements;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ImportSteamCatalogService(List<PlatformCatalogPort> catalogPorts, GameRepositoryPort games,
                                     GameMappingRepositoryPort gameMappings,
                                     AchievementRepositoryPort achievements,
                                     TransactionTemplate tx, Clock clock) {
        this.steam = catalogPorts.stream()
                .filter(p -> p.platform() == Platform.STEAM)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Steam catalog adapter registered"));
        this.games = games;
        this.gameMappings = gameMappings;
        this.achievements = achievements;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public long importGame(String steamAppId) {
        PlatformGameInfo info = steam.fetchGameInfo(steamAppId)
                .orElseThrow(() -> new IllegalArgumentException("Steam app not found: " + steamAppId));
        List<PlatformAchievement> steamAchievements = steam.fetchAchievements(steamAppId);
        Instant now = clock.instant();

        return tx.execute(status -> {
            long gameId = gameMappings.findByPlatformAndPlatformGameIds(Platform.STEAM, List.of(steamAppId))
                    .stream().findFirst()
                    .map(GameMapping::gameId)
                    .orElseGet(() -> createGame(info, now));

            achievements.saveAll(steamAchievements.stream()
                    .map(a -> new Achievement(null, gameId, a.platformAchievementId(), a.name(),
                            a.description(), a.iconUrl(), a.hidden(), now))
                    .toList());
            return gameId;
        });
    }

    private long createGame(PlatformGameInfo info, Instant now) {
        Game game = games.save(new Game(null, info.title(), info.coverImageUrl(), now));
        gameMappings.save(new GameMapping(null, game.id(), Platform.STEAM, info.platformGameId(), info.title()));
        return game.id();
    }
}
