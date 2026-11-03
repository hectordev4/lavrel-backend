package com.lavrel.user.application.service;

import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformRegistry;
import com.lavrel.user.application.port.in.CreatePlatformLinkUrlUseCase;
import com.lavrel.user.application.port.out.LinkStateSignerPort;
import com.lavrel.user.application.port.out.PlatformIdentityPort;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.UserNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CreatePlatformLinkUrlService implements CreatePlatformLinkUrlUseCase {

    private final PlatformRegistry<PlatformIdentityPort> identities;
    private final LinkStateSignerPort stateSigner;
    private final UserRepositoryPort users;

    public CreatePlatformLinkUrlService(List<PlatformIdentityPort> identities, LinkStateSignerPort stateSigner,
                                        UserRepositoryPort users) {
        this.identities = new PlatformRegistry<>(identities, "account linking");
        this.stateSigner = stateSigner;
        this.users = users;
    }

    @Override
    public String createLoginUrl(long userId, Platform platform) {
        PlatformIdentityPort identity = identities.require(platform);
        users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return identity.loginUrl(stateSigner.sign(userId, platform));
    }
}
