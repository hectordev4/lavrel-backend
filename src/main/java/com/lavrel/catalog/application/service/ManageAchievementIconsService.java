package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.ManageAchievementIconsUseCase;
import com.lavrel.catalog.application.port.out.AchievementIconPort;
import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformAchievement;
import com.lavrel.catalog.domain.GameMapping;
import com.lavrel.catalog.domain.GameNotFoundException;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ManageAchievementIconsService implements ManageAchievementIconsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageAchievementIconsService.class);

    private final AchievementIconPort icons;
    private final GameMappingRepositoryPort mappings;
    private final PlatformRegistry<PlatformCatalogPort> catalogs;
    private final String imageBase;

    public ManageAchievementIconsService(AchievementIconPort icons, GameMappingRepositoryPort mappings,
                                         List<PlatformCatalogPort> catalogs,
                                         @Value("${app.steam.image-base:https://shared.fastly.steamstatic.com/community_assets/images/apps/}")
                                         String imageBase) {
        this.icons = icons;
        this.mappings = mappings;
        this.catalogs = new PlatformRegistry<>(catalogs, "achievement icons");
        this.imageBase = imageBase.endsWith("/") ? imageBase : imageBase + "/";
    }

    @Override
    public int useConfiguredImageHost() {
        int moved = icons.useSteamImageBase(imageBase);
        if (moved > 0) {
            log.info("Moved {} achievement icons to {}", moved, imageBase);
        }
        return moved;
    }

    @Override
    public IconReport refreshFromSteam(long gameId) {
        GameMapping mapping = mappings.findByGameIdAndPlatform(gameId, Platform.STEAM)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        int updated = 0;
        int unknown = 0;
        for (PlatformAchievement a : catalogs.require(Platform.STEAM).fetchAchievements(mapping.platformGameId())) {
            if (a.iconUrl() == null || a.iconUrl().isBlank()) {
                continue;
            }
            if (icons.updateFromSteam(gameId, a.platformAchievementId(), a.iconUrl())) {
                updated++;
            } else {
                unknown++;
            }
        }
        log.info("Refreshed {} achievement icons of game {} from Steam ({} unknown)", updated, gameId, unknown);
        return new IconReport(updated, unknown);
    }
}
