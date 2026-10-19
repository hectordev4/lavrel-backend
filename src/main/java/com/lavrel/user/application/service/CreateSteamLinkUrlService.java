package com.lavrel.user.application.service;

import com.lavrel.user.application.port.in.CreateSteamLinkUrlUseCase;
import com.lavrel.user.application.port.out.LinkStateSignerPort;
import com.lavrel.user.application.port.out.SteamIdentityPort;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.UserNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CreateSteamLinkUrlService implements CreateSteamLinkUrlUseCase {

    private final SteamIdentityPort steamIdentity;
    private final LinkStateSignerPort stateSigner;
    private final UserRepositoryPort users;

    public CreateSteamLinkUrlService(SteamIdentityPort steamIdentity, LinkStateSignerPort stateSigner,
                                     UserRepositoryPort users) {
        this.steamIdentity = steamIdentity;
        this.stateSigner = stateSigner;
        this.users = users;
    }

    @Override
    public String createLoginUrl(long userId) {
        users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return steamIdentity.loginUrl(stateSigner.sign(userId));
    }
}
