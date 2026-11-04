package com.lavrel.user.application.port.in;

import com.lavrel.shared.domain.Platform;
import com.lavrel.user.domain.PlatformAccountNotFoundException;

public interface UnlinkPlatformAccountUseCase {

    /**
     * Unlinks one of the user's accounts: its games leave the library (they stay hidden in collections) and the
     * unlocks recorded from it are removed. Tracked games and collections stay. Linking it again and syncing brings
     * everything back.
     *
     * @throws PlatformAccountNotFoundException if it is not linked to the user
     */
    void unlink(long userId, Platform platform, String externalId);
}
