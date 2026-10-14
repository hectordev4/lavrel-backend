package com.lavrel.user.domain;

import com.lavrel.shared.domain.Platform;
import java.time.Instant;
import java.util.Objects;

/**
 * MIXED. The link is app-created; platform and externalId come from the platform.
 * Unique on (platform, externalId). Only created after the platform confirmed ownership
 * (Steam OpenID), so verified is true for every persisted account in the MVP.
 */
public record PlatformAccount(
        Long id,
        long userId,
        Platform platform,
        String externalId,
        String displayName,
        boolean verified,
        Instant verifiedAt,
        Instant linkedAt,
        Instant lastSyncedAt) {

    public PlatformAccount {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(linkedAt, "linkedAt");
        if (verified && verifiedAt == null) {
            throw new IllegalArgumentException("verifiedAt is required when verified");
        }
    }

    public static PlatformAccount linkVerified(long userId, Platform platform, String externalId,
                                               String displayName, Instant now) {
        return new PlatformAccount(null, userId, platform, externalId, displayName, true, now, now, null);
    }

    public PlatformAccount markSynced(Instant at) {
        return new PlatformAccount(id, userId, platform, externalId, displayName,
                verified, verifiedAt, linkedAt, at);
    }
}
