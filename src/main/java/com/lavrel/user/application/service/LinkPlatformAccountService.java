package com.lavrel.user.application.service;

import com.lavrel.shared.domain.PlatformRegistry;
import com.lavrel.user.application.port.in.LinkPlatformAccountUseCase;
import com.lavrel.user.application.port.out.LinkStateSignerPort;
import com.lavrel.user.application.port.out.PlatformAccountRepositoryPort;
import com.lavrel.user.application.port.out.PlatformIdentityPort;
import com.lavrel.user.application.port.out.PlatformIdentityPort.PlatformIdentity;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.PlatformAccount;
import com.lavrel.user.domain.UserNotFoundException;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * No @Transactional on purpose: the platform verification is a network call and must not hold a
 * database transaction. The single save is atomic by itself, and the unique constraint on
 * (platform, externalId) is what rejects a duplicate link.
 */
@Service
public class LinkPlatformAccountService implements LinkPlatformAccountUseCase {

    private final PlatformRegistry<PlatformIdentityPort> identities;
    private final LinkStateSignerPort stateSigner;
    private final PlatformAccountRepositoryPort accounts;
    private final UserRepositoryPort users;
    private final Clock clock;

    public LinkPlatformAccountService(List<PlatformIdentityPort> identities, LinkStateSignerPort stateSigner,
                                      PlatformAccountRepositoryPort accounts, UserRepositoryPort users,
                                      Clock clock) {
        this.identities = new PlatformRegistry<>(identities, "account linking");
        this.stateSigner = stateSigner;
        this.accounts = accounts;
        this.users = users;
        this.clock = clock;
    }

    @Override
    public PlatformAccount link(Command command) {
        PlatformIdentityPort identityProvider = identities.require(command.platform());
        long userId = stateSigner.verify(command.state(), command.platform());
        users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        PlatformIdentity identity = identityProvider.verify(command.callbackParams());
        PlatformAccount account = PlatformAccount.linkVerified(userId, command.platform(),
                identity.externalId(), identity.displayName(), clock.instant());
        return accounts.save(account);
    }
}
