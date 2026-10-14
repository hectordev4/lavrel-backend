package com.lavrel.user.domain;

/** The Firebase ID token is missing, expired or invalid. Maps to HTTP 401. */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
