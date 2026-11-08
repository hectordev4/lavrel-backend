package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.ManageAchievementIconsUseCase;
import com.lavrel.catalog.application.port.out.AchievementIconPort;
import com.lavrel.catalog.application.port.out.AchievementIconPort.IconRef;
import com.lavrel.catalog.application.port.out.IconFilesPort;
import com.lavrel.catalog.application.port.out.IconFilesPort.IconFile;
import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformAchievement;
import com.lavrel.catalog.domain.GameMapping;
import com.lavrel.catalog.domain.GameNotFoundException;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ManageAchievementIconsService implements ManageAchievementIconsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageAchievementIconsService.class);

    private final AchievementIconPort icons;
    private final IconFilesPort files;
    private final GameMappingRepositoryPort mappings;
    private final PlatformRegistry<PlatformCatalogPort> catalogs;
    private final String imageBase;

    public ManageAchievementIconsService(AchievementIconPort icons, IconFilesPort files,
                                         GameMappingRepositoryPort mappings,
                                         List<PlatformCatalogPort> catalogs,
                                         @Value("${app.steam.image-base:https://shared.fastly.steamstatic.com/community_assets/images/apps/}")
                                         String imageBase) {
        this.icons = icons;
        this.files = files;
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
    public MirrorReport mirror(long gameId) {
        int copied = 0;
        int already = 0;
        List<Long> missing = new ArrayList<>();
        for (IconRef icon : icons.findByGame(gameId)) {
            if (icon.mirrored()) {
                already++;
                continue;
            }
            Optional<IconFile> file = files.downloadFromSteam(icon.iconPath());
            if (file.isEmpty()) {
                missing.add(icon.achievementId());
                continue;
            }
            files.store(icon.iconPath(), file.get());
            icons.useMirror(icon.achievementId(), files.publicUrl(icon.iconPath()));
            copied++;
        }
        log.info("Copied {} icons of game {} into our storage ({} already, {} missing on Steam)", copied, gameId,
                already, missing.size());
        return new MirrorReport(copied, already, missing);
    }

    @Override
    public int unmirror(long gameId) {
        int count = 0;
        for (IconRef icon : icons.findByGame(gameId)) {
            if (icon.mirrored()) {
                icons.useSteam(icon.achievementId());
                files.delete(icon.iconPath());
                count++;
            }
        }
        log.info("Game {} serves {} icons from Steam again", gameId, count);
        return count;
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
