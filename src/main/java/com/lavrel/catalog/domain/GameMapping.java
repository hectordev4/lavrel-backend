package com.lavrel.catalog.domain;

import com.lavrel.shared.domain.Platform;
import java.util.Objects;

/**
 * MIXED. platformGameId comes from the platform; the match to Game is app-curated.
 * Unique on (platform, platformGameId).
 */
public record GameMapping(Long id, long gameId, Platform platform, String platformGameId, String platformTitle) {
    public GameMapping {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(platformGameId, "platformGameId");
    }
}
