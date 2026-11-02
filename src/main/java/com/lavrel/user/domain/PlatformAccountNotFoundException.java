package com.lavrel.user.domain;

import com.lavrel.shared.domain.Platform;

/**
 * No such linked account, or it belongs to somebody else (the two are not told apart, so nobody can probe whose
 * accounts exist). The message never contains the database id: it reaches the client.
 */
public class PlatformAccountNotFoundException extends RuntimeException {

    public PlatformAccountNotFoundException(long platformAccountId) {
        super("Platform account not found");
    }

    public PlatformAccountNotFoundException(Platform platform, String externalId) {
        super("No linked " + platform + " account " + externalId);
    }
}
