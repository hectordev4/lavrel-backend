package catalog.application.port.in;

import shared.domain.Platform;

public interface CompareAchievementsUseCase {

    /**
     * Compares the platform's achievement list for the game against our Steam-based list
     * and stores one AchievementMapping per Steam achievement (available true/false).
     *
     * @return number of achievements matched
     */
    int compare(long gameId, Platform platform);
}
