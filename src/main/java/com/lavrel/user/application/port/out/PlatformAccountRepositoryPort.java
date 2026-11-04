package com.lavrel.user.application.port.out;

import com.lavrel.shared.domain.Platform;
import com.lavrel.user.domain.AccountAlreadyLinkedException;
import com.lavrel.user.domain.PlatformAccount;
import java.util.List;
import java.util.Optional;

public interface PlatformAccountRepositoryPort {

    Optional<PlatformAccount> findById(long id);

    List<PlatformAccount> findByUserId(long userId);

    Optional<PlatformAccount> findByPlatformAndExternalId(Platform platform, String externalId);

    /**
     * @throws AccountAlreadyLinkedException if (platform, externalId) already exists.
     *         The adapter translates the unique-constraint violation; no check-then-insert.
     */
    PlatformAccount save(PlatformAccount account);

    void delete(long platformAccountId);
}
