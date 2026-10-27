package com.lavrel.shared.domain;

/** Anything that works for exactly one platform: adapters, importers, identity providers. */
public interface PlatformBound {

    Platform platform();
}
