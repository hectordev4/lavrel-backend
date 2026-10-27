package com.lavrel.shared.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The one place that picks the implementation for a platform (Steam, Xbox, PSN).
 * Services receive every implementation of a port as a list, wrap it in a registry and ask it for the platform
 * they need, so adding Xbox or PSN means adding an adapter, not editing the services.
 * Registering two implementations for the same platform fails at startup.
 *
 * @param <T> the port, for example PlatformLibraryPort
 */
public final class PlatformRegistry<T extends PlatformBound> {

    private final Map<Platform, T> byPlatform;
    private final String capability;

    /** @param capability what the port does, used in error messages ("library sync", "account linking"...) */
    public PlatformRegistry(Collection<? extends T> implementations, String capability) {
        Map<Platform, T> map = new EnumMap<>(Platform.class);
        for (T implementation : implementations) {
            T previous = map.put(implementation.platform(), implementation);
            if (previous != null) {
                throw new IllegalStateException("Two " + capability + " implementations for "
                        + implementation.platform() + ": " + previous.getClass().getSimpleName()
                        + " and " + implementation.getClass().getSimpleName());
            }
        }
        this.byPlatform = Collections.unmodifiableMap(map);
        this.capability = capability;
    }

    /** @throws PlatformNotSupportedException if nothing is registered for the platform */
    public T require(Platform platform) {
        T implementation = byPlatform.get(platform);
        if (implementation == null) {
            throw new PlatformNotSupportedException(platform, capability);
        }
        return implementation;
    }

    public Optional<T> find(Platform platform) {
        return Optional.ofNullable(byPlatform.get(platform));
    }

    public boolean supports(Platform platform) {
        return byPlatform.containsKey(platform);
    }

    public Set<Platform> platforms() {
        return byPlatform.keySet();
    }
}
