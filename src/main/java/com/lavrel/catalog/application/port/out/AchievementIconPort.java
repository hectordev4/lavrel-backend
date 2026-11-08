package com.lavrel.catalog.application.port.out;

import java.util.List;

/**
 * The icons of achievements (migration V31). The database keeps each Steam icon's reference ("{appId}/{file}") and
 * builds the served address from the host in app_setting, so most of the work happens there; these are the few
 * operations the application starts.
 */
public interface AchievementIconPort {

    /** Saves the Steam image host and moves every non-mirrored icon onto it. @return how many icons moved */
    int useSteamImageBase(String base);

    /**
     * Gives an achievement the icon Steam lists now (the database turns it into a reference on the current host).
     * @return false if the game has no achievement with that Steam id
     */
    boolean updateFromSteam(long gameId, String steamAchievementId, String steamIconUrl);

    /** The icons of a game that have a Steam reference. */
    List<IconRef> findByGame(long gameId);

    /** Serves our own copy from now on. */
    void useMirror(long achievementId, String url);

    /** Back to Steam's host. */
    void useSteam(long achievementId);

    /** iconPath: "{appId}/{file}"; mirrored: served from our own storage. */
    record IconRef(long achievementId, String iconPath, boolean mirrored) {
    }
}
