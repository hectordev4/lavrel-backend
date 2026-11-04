package com.lavrel.progress.application.port.in;

import com.lavrel.catalog.domain.GameNotFoundException;
import java.time.Instant;
import java.util.List;

/**
 * The full achievement list of a game, for the game page. Public: anyone sees the list and the worldwide unlock
 * percentages; a signed-in user also sees which ones they unlocked, and the percentage in their own country.
 */
public interface GetGameAchievementsUseCase {

    /**
     * @param userId null for an anonymous visitor
     * @throws GameNotFoundException if the game is not published
     */
    List<AchievementItem> list(long gameId, Long userId);

    /**
     * globalPercent is null while nobody owns the game; countryPercent is null without a signed-in user with a
     * country, or when too few players from that country own the game. unlockedAt is null when not unlocked (or
     * anonymous). hidden achievements should be shown blurred until tapped.
     */
    record AchievementItem(long id, String name, String description, String iconUrl, boolean hidden,
                           Double globalPercent, Double countryPercent, Instant unlockedAt) {
    }
}
