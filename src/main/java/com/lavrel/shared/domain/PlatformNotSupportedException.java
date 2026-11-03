package com.lavrel.shared.domain;

/** The platform exists in our model, but nothing is wired up for it yet (no adapter registered). */
public class PlatformNotSupportedException extends RuntimeException {

    private final Platform platform;

    public PlatformNotSupportedException(Platform platform, String capability) {
        super(platform + " is not supported yet for " + capability);
        this.platform = platform;
    }

    public Platform platform() {
        return platform;
    }
}
