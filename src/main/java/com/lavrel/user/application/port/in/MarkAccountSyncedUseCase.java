package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.PlatformAccountNotFoundException;

public interface MarkAccountSyncedUseCase {

    /**
     * Records that the account's library was just read (lastSyncedAt = now).
     *
     * @throws PlatformAccountNotFoundException if the account does not exist
     */
    void markSynced(long platformAccountId);
}
