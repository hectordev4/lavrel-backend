package com.lavrel.library.application.port.in;

import com.lavrel.library.domain.ProfilePrivateException;

public interface SyncAccountUseCase {

    /**
     * Syncs the games and unlocks of one platform account.
     * The account must belong to userId.
     *
     * @throws ProfilePrivateException if the platform profile is private
     */
    SyncResult sync(long userId, long platformAccountId);

    record SyncResult(int gamesSynced, int gamesSkipped, int unlocksRecorded) {
    }
}
