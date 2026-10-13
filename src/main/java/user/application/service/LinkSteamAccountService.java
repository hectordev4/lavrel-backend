package user.application.service;

import java.time.Clock;
import org.springframework.stereotype.Service;
import shared.domain.Platform;
import user.application.port.in.LinkSteamAccountUseCase;
import user.application.port.out.PlatformAccountRepositoryPort;
import user.application.port.out.SteamIdentityPort;
import user.application.port.out.SteamIdentityPort.SteamIdentity;
import user.application.port.out.UserRepositoryPort;
import user.domain.PlatformAccount;
import user.domain.UserNotFoundException;

/**
 * No @Transactional on purpose: the Steam verification is a network call and must not hold a
 * database transaction. The single save is atomic by itself, and the unique constraint on
 * (platform, externalId) is what rejects a duplicate link.
 */
@Service
public class LinkSteamAccountService implements LinkSteamAccountUseCase {

    private final SteamIdentityPort steamIdentity;
    private final PlatformAccountRepositoryPort accounts;
    private final UserRepositoryPort users;
    private final Clock clock;

    public LinkSteamAccountService(SteamIdentityPort steamIdentity, PlatformAccountRepositoryPort accounts,
                                   UserRepositoryPort users, Clock clock) {
        this.steamIdentity = steamIdentity;
        this.accounts = accounts;
        this.users = users;
        this.clock = clock;
    }

    @Override
    public PlatformAccount link(Command command) {
        users.findById(command.userId()).orElseThrow(() -> new UserNotFoundException(command.userId()));
        SteamIdentity identity = steamIdentity.verifyOpenId(command.openIdParams());
        PlatformAccount account = PlatformAccount.linkVerified(command.userId(), Platform.STEAM,
                identity.steamId64(), identity.personaName(), clock.instant());
        return accounts.save(account);
    }
}
