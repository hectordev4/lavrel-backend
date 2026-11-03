package com.lavrel.library.domain;

/** The platform profile is private, so its games and achievements cannot be read. */
public class ProfilePrivateException extends RuntimeException {
    public ProfilePrivateException(String externalId) {
        super("Profile is private: " + externalId);
    }
}
