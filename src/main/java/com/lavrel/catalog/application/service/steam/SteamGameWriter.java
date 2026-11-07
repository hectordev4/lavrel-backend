package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.out.AchievementRepositoryPort;
import com.lavrel.catalog.application.port.out.FranchiseBlocklistPort;
import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.GameMetadataRepositoryPort;
import com.lavrel.catalog.application.port.out.GameRepositoryPort;
import com.lavrel.catalog.application.port.out.GameSystemRepositoryPort;
import com.lavrel.catalog.application.port.out.GameSystemRepositoryPort.Source;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformAchievement;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformGameInfo;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.StoreItem;
import com.lavrel.catalog.domain.Achievement;
import com.lavrel.catalog.domain.Game;
import com.lavrel.catalog.domain.GameMapping;
import com.lavrel.catalog.domain.GameMetadata;
import com.lavrel.catalog.domain.GameSystem;
import com.lavrel.shared.domain.Platform;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Writes Steam games into the catalog. Each game and its details go in one short transaction; the Steam calls
 * that produced the data happened before, outside any transaction. Saving the details is what publishes a game.
 */
@Component
class SteamGameWriter {

    private final GameRepositoryPort games;
    private final GameMappingRepositoryPort gameMappings;
    private final AchievementRepositoryPort achievements;
    private final GameMetadataRepositoryPort metadata;
    private final GameSystemRepositoryPort systems;
    private final FranchiseBlocklistPort franchiseBlocklist;
    private final SteamTagNames tagNames;
    private final TransactionTemplate tx;
    private final Clock clock;

    SteamGameWriter(GameRepositoryPort games, GameMappingRepositoryPort gameMappings,
                    AchievementRepositoryPort achievements, GameMetadataRepositoryPort metadata,
                    GameSystemRepositoryPort systems, FranchiseBlocklistPort franchiseBlocklist,
                    SteamTagNames tagNames, TransactionTemplate tx, Clock clock) {
        this.games = games;
        this.gameMappings = gameMappings;
        this.achievements = achievements;
        this.metadata = metadata;
        this.systems = systems;
        this.franchiseBlocklist = franchiseBlocklist;
        this.tagNames = tagNames;
        this.tx = tx;
        this.clock = clock;
    }

    /** Creates a new published game with its Steam mapping, achievements and details. @return the game id */
    Long insert(String appId, PlatformGameInfo info, StoreItem item, List<PlatformAchievement> steamAchievements) {
        Instant now = clock.instant();
        GameMetadata gameMetadata = GameMetadataAssembler.assemble(info, item, franchiseBlocklist.keys());
        tagNames.ensureNames(gameMetadata.tags());

        return tx.execute(status -> {
            Game game = games.save(Game.create(info.title(), info.coverImageUrl(), info.description(),
                    storeUrl(appId), now));
            gameMappings.save(new GameMapping(null, game.id(), Platform.STEAM, appId, info.title()));
            achievements.saveAll(steamAchievements.stream()
                    .map(a -> new Achievement(null, game.id(), a.platformAchievementId(), a.name(),
                            a.description(), a.iconUrl(), a.hidden(), now))
                    .toList());
            metadata.save(game.id(), gameMetadata);
            systems.add(game.id(), GameSystem.PC, Source.IMPORTED);
            return game.id();
        });
    }

    /** Rewrites the details of an existing game (publishing it if it was pending). */
    void republish(long gameId, String appId, PlatformGameInfo info, StoreItem item) {
        Game game = games.findById(gameId).orElse(null);
        if (game == null) {
            return;
        }
        GameMetadata gameMetadata = GameMetadataAssembler.assemble(info, item, franchiseBlocklist.keys());
        tagNames.ensureNames(gameMetadata.tags());
        tx.executeWithoutResult(status -> {
            game.refreshFromPlatform(info.title(), info.coverImageUrl(), info.description(), storeUrl(appId));
            games.save(game);
            metadata.save(gameId, gameMetadata);
            systems.add(gameId, GameSystem.PC, Source.IMPORTED);
        });
    }

    private static String storeUrl(String steamAppId) {
        return "https://store.steampowered.com/app/" + steamAppId;
    }
}
