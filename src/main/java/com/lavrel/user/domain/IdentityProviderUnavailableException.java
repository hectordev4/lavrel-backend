package com.lavrel.user.domain;

/** The identity provider (Firebase) could not be reached or refused an account change. Maps to HTTP 502. */
public class IdentityProviderUnavailableException extends RuntimeException {

    public IdentityProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
