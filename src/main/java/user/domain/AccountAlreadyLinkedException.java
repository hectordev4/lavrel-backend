package user.domain;

import shared.domain.Platform;

public class AccountAlreadyLinkedException extends RuntimeException {
    public AccountAlreadyLinkedException(Platform platform, String externalId) {
        super("Account already linked: " + platform + " / " + externalId);
    }
}