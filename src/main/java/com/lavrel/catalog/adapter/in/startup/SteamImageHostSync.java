package com.lavrel.catalog.adapter.in.startup;

import com.lavrel.catalog.application.port.in.ManageAchievementIconsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On every start, applies app.steam.image-base: if Steam moves its images again, changing that setting (env
 * APP_STEAM_IMAGE_BASE) and restarting moves every icon. A failure is logged and never stops the app.
 */
@Component
public class SteamImageHostSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SteamImageHostSync.class);

    private final ManageAchievementIconsUseCase icons;

    public SteamImageHostSync(ManageAchievementIconsUseCase icons) {
        this.icons = icons;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            icons.useConfiguredImageHost();
        } catch (RuntimeException e) {
            log.warn("Could not apply the Steam image host: {}", e.toString());
        }
    }
}
