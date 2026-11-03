package com.lavrel.user.application.port.in;

import com.lavrel.shared.domain.Platform;
import com.lavrel.user.domain.PlatformAccountNotFoundException;
import java.time.Instant;
import java.util.List;

/** Read access for other features (e.g. library). Returns a view, not the domain object. */
public interface GetPlatformAccountUseCase {

    /** @throws PlatformAccountNotFoundException if it does not exist */
    PlatformAccountView getById(long platformAccountId);

    List<PlatformAccountView> getByUserId(long userId);

    /**
     * One of the user's own accounts, addressed the way the API does: by platform and the platform's account id.
     * @throws PlatformAccountNotFoundException if it is not linked, or linked to somebody else
     */
    PlatformAccountView getOwned(long userId, Platform platform, String externalId);

    /** lastSyncedAt is null until the first sync. */
    record PlatformAccountView(long id, long userId, Platform platform, String externalId, String displayName,
                               Instant lastSyncedAt) {
    }
}
