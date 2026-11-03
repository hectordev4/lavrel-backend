package com.lavrel.shared.domain;

/** A platform API (Steam, Xbox, PSN) could not be reached or returned something unusable. Maps to HTTP 502. */
public class PlatformUnavailableException extends RuntimeException {

    public PlatformUnavailableException(Platform platform, String message) {
        super(platform + " API unavailable: " + message);
    }

    public PlatformUnavailableException(Platform platform, String message, Throwable cause) {
        super(platform + " API unavailable: " + message, cause);
    }
}
