package com.lavrel.user.application.service;

import com.lavrel.shared.domain.PlatformRegistry;
import com.lavrel.user.application.port.in.LinkPlatformAccountUseCase;
import com.lavrel.user.application.port.out.LinkStateSignerPort;
import com.lavrel.user.application.port.out.PlatformAccountRepositoryPort;
import com.lavrel.user.application.port.out.PlatformIdentityPort.PlatformIdentity;
import com.lavrel.user.application.port.out.PlatformIdentityPort;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.AccountAlreadyLinkedException;
import com.lavrel.user.domain.AccountLinkedElsewhereException;
import com.lavrel.user.domain.PlatformAccount;
import com.lavrel.user.domain.UserNotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * No @Transactional on purpose: the platform verification is a network call and must not hold a
 * database transaction. The single save is atomic by itself, and the unique constraint on
 * (platform, externalId) is what rejects a duplicate link.
 *
 * Linking is safe to repeat: an account already linked to the same user is simply returned (an old tab, a retry), and
 * one linked to another user is AccountLinkedElsewhereException, which never says who has it. Two links racing for
 * the same account are settled by the unique constraint and the same check.
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
        Optional<PlatformAccount> existing = accounts.findByPlatformAndExternalId(command.platform(),
                identity.externalId());
        if (existing.isPresent()) {
            return ownedBy(userId, existing.get());
        }
        PlatformAccount account = PlatformAccount.linkVerified(userId, command.platform(),
                identity.externalId(), identity.displayName(), clock.instant());
        try {
            return accounts.save(account);
        } catch (AccountAlreadyLinkedException raced) {
            return accounts.findByPlatformAndExternalId(command.platform(), identity.externalId())
                    .map(found -> ownedBy(userId, found))
                    .orElseThrow(AccountLinkedElsewhereException::new);
        }
    }

    private static PlatformAccount ownedBy(long userId, PlatformAccount account) {
        if (!account.belongsTo(userId)) {
            throw new AccountLinkedElsewhereException();
        }
        return account;
    }
}
