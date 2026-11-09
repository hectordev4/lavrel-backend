package com.lavrel.progress.application.port.in;

import java.time.Instant;
import java.util.List;

/**
 * The achievement picker for profile shelves: first the games where the user unlocked something, then that game's
 * unlocked achievements. The chosen ids go to PUT /api/currentuser/profile/shelves/{id}/achievements. Private.
 */
public interface PickUnlocksUseCase {

    int DEFAULT_PAGE_SIZE = 24;
    int MAX_PAGE_SIZE = 60;

    /** query: optional part of the name (accent and case insensitive). */
    GamesPage gamesWithUnlocks(long userId, String query, GameSort sort, int page, int size);

    /** Every achievement the user unlocked in that game, in the chosen order (empty if none). */
    List<UnlockedAchievement> unlockedIn(long userId, long gameId, AchievementSort sort);

    /** RECENT (default): last unlock first. NAME: alphabetical. */
    enum GameSort {
        RECENT, NAME
    }

    /** RARITY (default): rarest first (lowest share of owners), unknown rarity last. DATE: newest unlock first. */
    enum AchievementSort {
        RARITY, DATE
    }

    record GamesPage(List<GameWithUnlocks> games, int page, int size, boolean hasMore) {
    }

    record GameWithUnlocks(long gameId, String title, String slug, String coverImageUrl, int unlocked, int total,
                           Instant lastUnlockedAt) {
    }

    /** globalPercent: share of the game's owners who unlocked it; null while nobody owns the game. */
    record UnlockedAchievement(long achievementId, String name, String iconUrl, boolean hidden, Double globalPercent,
                               Instant unlockedAt) {
    }
}
