package com.lavrel.user.application.service;

import com.lavrel.shared.domain.Platform;
import com.lavrel.user.application.port.in.LinkSteamAccountUseCase;
import com.lavrel.user.application.port.out.LinkStateSignerPort;
import com.lavrel.user.application.port.out.PlatformAccountRepositoryPort;
import com.lavrel.user.application.port.out.SteamIdentityPort;
import com.lavrel.user.application.port.out.SteamIdentityPort.SteamIdentity;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.PlatformAccount;
import com.lavrel.user.domain.UserNotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * No @Transactional on purpose: the Steam verification is a network call and must not hold a
 * database transaction. The single save is atomic by itself, and the unique constraint on
 * (platform, externalId) is what rejects a duplicate link.
 */
@Service
public class LinkSteamAccountService implements LinkSteamAccountUseCase {

    private final LinkStateSignerPort stateSigner;
    private final SteamIdentityPort steamIdentity;
    private final PlatformAccountRepositoryPort accounts;
    private final UserRepositoryPort users;
    private final Clock clock;

    public LinkSteamAccountService(LinkStateSignerPort stateSigner, SteamIdentityPort steamIdentity,
                                   PlatformAccountRepositoryPort accounts, UserRepositoryPort users,
                                   Clock clock) {
        this.stateSigner = stateSigner;
        this.steamIdentity = steamIdentity;
        this.accounts = accounts;
        this.users = users;
        this.clock = clock;
    }

    @Override
    public PlatformAccount link(Command command) {
        long userId = stateSigner.verify(command.state());
        users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        SteamIdentity identity = steamIdentity.verifyOpenId(command.openIdParams());
        PlatformAccount account = PlatformAccount.linkVerified(userId, Platform.STEAM,
                identity.steamId64(), identity.personaName(), clock.instant());
        return accounts.save(account);
    }
}