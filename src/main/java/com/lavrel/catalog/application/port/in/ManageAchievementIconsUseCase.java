package com.lavrel.catalog.application.port.in;

import com.lavrel.catalog.domain.GameNotFoundException;
import java.util.List;

/** Admin: keeps achievement icons working when Steam moves or changes its images. */
public interface ManageAchievementIconsUseCase {

    /** At start-up: makes the configured Steam image host current and moves every icon onto it. */
    int useConfiguredImageHost();

    /**
     * Reads the game's achievement list from Steam again and updates the icons of the achievements we have
     * (an icon copied into our storage is kept while the Steam picture is the same).
     * @throws GameNotFoundException if the game has no Steam mapping
     */
    IconReport refreshFromSteam(long gameId);

    /**
     * Copies the game's icons into our own storage and serves them from there (for icons Steam lost or may lose).
     * Icons Steam no longer has on any of its hosts are listed as missing and left as they are.
     * @throws com.lavrel.shared.storage.StorageUnavailableException if storage is not configured
     */
    MirrorReport mirror(long gameId);

    /** Serves the game's icons from Steam again and deletes our copies. @return how many */
    int unmirror(long gameId);

    record MirrorReport(int copied, int alreadyCopied, List<Long> missingAchievementIds) {
    }

    /** updated: icons rewritten; unknown: achievements Steam lists that we do not have (left alone). */
    record IconReport(int updated, int unknown) {
    }
}
