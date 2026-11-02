package com.lavrel.user.domain;

import com.lavrel.shared.domain.Platform;
import java.time.Instant;
import java.util.Objects;

/**
 * A game-platform account (Steam, Xbox, PSN) linked to a user. The link is ours; platform and externalId come from
 * the platform. Unique on (platform, externalId), which is also how the API addresses it: the database id never
 * leaves the backend. An account is only created after the platform confirmed who owns it (Steam OpenID), so
 * verified is true for every stored account for now.
 *
 * An entity: its identity is the platform account itself (platform and externalId), and it changes over time
 * (markSynced). Create it with {@link #linkVerified}; the persistence adapter rebuilds stored ones with
 * {@link #restore}.
 */
public class PlatformAccount {

    private final Long id;
    private final long userId;
    private final Platform platform;
    private final String externalId;
    private final String displayName;
    private final boolean verified;
    private final Instant verifiedAt;
    private final Instant linkedAt;
    private Instant lastSyncedAt;

    private PlatformAccount(Long id, long userId, Platform platform, String externalId, String displayName,
                            boolean verified, Instant verifiedAt, Instant linkedAt, Instant lastSyncedAt) {
        if (verified && verifiedAt == null) {
            throw new IllegalArgumentException("verifiedAt is required when verified");
        }
        this.id = id;
        this.userId = userId;
        this.platform = Objects.requireNonNull(platform, "platform");
        this.externalId = Objects.requireNonNull(externalId, "externalId");
        this.displayName = displayName;
        this.verified = verified;
        this.verifiedAt = verifiedAt;
        this.linkedAt = Objects.requireNonNull(linkedAt, "linkedAt");
        this.lastSyncedAt = lastSyncedAt;
    }

    /** A new account the platform has just confirmed belongs to the user. Never synced yet, no id yet. */
    public static PlatformAccount linkVerified(long userId, Platform platform, String externalId,
                                               String displayName, Instant now) {
        return new PlatformAccount(null, userId, platform, externalId, displayName, true, now, now, null);
    }

    /** Rebuilds a stored account. For persistence adapters only. */
    public static PlatformAccount restore(Long id, long userId, Platform platform, String externalId,
                                          String displayName, boolean verified, Instant verifiedAt,
                                          Instant linkedAt, Instant lastSyncedAt) {
        return new PlatformAccount(id, userId, platform, externalId, displayName, verified, verifiedAt,
                linkedAt, lastSyncedAt);
    }

    // ------------------------------------------------------------------ behaviour

    /** Records that the library of this account was just read. */
    public void markSynced(Instant at) {
        this.lastSyncedAt = Objects.requireNonNull(at, "at");
    }

    /** True when this account belongs to that user (used before acting on someone's behalf). */
    public boolean belongsTo(long someUserId) {
        return userId == someUserId;
    }

    // ------------------------------------------------------------------ reading

    /** null until the account is stored. Never shown outside the backend. */
    public Long id() {
        return id;
    }

    public long userId() {
        return userId;
    }

    public Platform platform() {
        return platform;
    }

    public String externalId() {
        return externalId;
    }

    /** The persona or gamertag shown in the UI. May be null. */
    public String displayName() {
        return displayName;
    }

    public boolean verified() {
        return verified;
    }

    public Instant verifiedAt() {
        return verifiedAt;
    }

    public Instant linkedAt() {
        return linkedAt;
    }

    /** null until the first sync. */
    public Instant lastSyncedAt() {
        return lastSyncedAt;
    }

    /** The same platform account, whoever holds it in memory. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof PlatformAccount account
                && platform == account.platform && externalId.equals(account.externalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platform, externalId);
    }

    @Override
    public String toString() {
        return "PlatformAccount{id=" + id + ", userId=" + userId + ", platform=" + platform
                + ", externalId=" + externalId + ", lastSyncedAt=" + lastSyncedAt + "}";
    }
}
