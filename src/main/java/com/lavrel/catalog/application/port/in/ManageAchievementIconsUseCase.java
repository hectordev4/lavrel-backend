package com.lavrel.catalog.application.port.in;

import com.lavrel.catalog.domain.GameNotFoundException;

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

    /** updated: icons rewritten; unknown: achievements Steam lists that we do not have (left alone). */
    record IconReport(int updated, int unknown) {
    }
}
