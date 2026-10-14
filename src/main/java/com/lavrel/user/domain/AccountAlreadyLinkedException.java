package com.lavrel.user.domain;

import com.lavrel.shared.domain.Platform;

public class AccountAlreadyLinkedException extends RuntimeException {
    public AccountAlreadyLinkedException(Platform platform, String externalId) {
        super("Account already linked: " + platform + " / " + externalId);
    }
}
