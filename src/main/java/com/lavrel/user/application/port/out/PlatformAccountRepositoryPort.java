package com.lavrel.user.application.port.out;

import java.util.List;
import java.util.Optional;
import com.lavrel.user.domain.AccountAlreadyLinkedException;
import com.lavrel.user.domain.PlatformAccount;

public interface PlatformAccountRepositoryPort {

    Optional<PlatformAccount> findById(long id);

    List<PlatformAccount> findByUserId(long userId);

    /**
     * @throws AccountAlreadyLinkedException if (platform, externalId) already exists.
     *         The adapter translates the unique-constraint violation; no check-then-insert.
     */
    PlatformAccount save(PlatformAccount account);
}
