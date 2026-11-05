package com.lavrel.library.application.port.in;

import com.lavrel.library.domain.ProfilePrivateException;
import com.lavrel.shared.domain.Platform;
import com.lavrel.user.domain.PlatformAccountNotFoundException;

public interface SyncAccountUseCase {

    /**
     * Reads the library of one platform account and brings the catalog, the owned games and the unlocks up to
     * date. Every game that is safe for work and has achievements is picked up; adult games and games without
     * achievements are left out. At most a limited number of NEW games are imported per sync: when
     * gamesPending is above 0, syncing again continues with the rest. The account is addressed by platform and the
     * platform's account id (SteamID64...), and must belong to userId.
     *
     * @throws ProfilePrivateException          if the platform profile is private
     * @throws PlatformAccountNotFoundException if the account is not linked to userId
     */
    SyncResult sync(long userId, Platform platform, String externalId);

    /**
     * The automatic sync of one account: reads the library, imports new games, and reads unlocks only for games
     * that are new to the account or were played since the last sync. Not exposed over HTTP.
     *
     * @throws ProfilePrivateException if the platform profile is private
     */
    SyncResult syncChangedGames(long platformAccountId);

    /**
     * gamesSynced: games of the library now owned in our database. gamesSkipped: library games left out (adult,
     * no achievements, or waiting). gamesPending: games still waiting to be imported because of the per-sync limit.
     */
    record SyncResult(int gamesSynced, int gamesSkipped, int unlocksRecorded, int gamesPending) {
    }
}
