package com.lavrel.tracking.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A game a user chose to follow, with its status. Added by the user only: a sync never adds games here.
 * pinPosition is set when the game is in the user's quick access (1 = first), null otherwise.
 *
 * An entity: identified by (userId, gameId), it changes over time through its methods. Create it with
 * {@link #track}; the persistence adapter rebuilds stored ones with {@link #restore}.
 */
public class TrackedGame {

    private final long userId;
    private final long gameId;
    private TrackingStatus status;
    private Integer pinPosition;
    private final Instant addedAt;
    private Instant updatedAt;

    private TrackedGame(long userId, long gameId, TrackingStatus status, Integer pinPosition, Instant addedAt,
                        Instant updatedAt) {
        this.userId = userId;
        this.gameId = gameId;
        this.status = Objects.requireNonNull(status, "status");
        this.pinPosition = pinPosition;
        this.addedAt = Objects.requireNonNull(addedAt, "addedAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static TrackedGame track(long userId, long gameId, TrackingStatus status, Instant now) {
        return new TrackedGame(userId, gameId, status, null, now, now);
    }

    /** Rebuilds a stored entry. For persistence adapters only. */
    public static TrackedGame restore(long userId, long gameId, TrackingStatus status, Integer pinPosition,
                                      Instant addedAt, Instant updatedAt) {
        return new TrackedGame(userId, gameId, status, pinPosition, addedAt, updatedAt);
    }

    // ------------------------------------------------------------------ behaviour

    /** @return true if it changed */
    public boolean changeStatus(TrackingStatus newStatus, Instant now) {
        Objects.requireNonNull(newStatus, "status");
        if (status == newStatus) {
            return false;
        }
        this.status = newStatus;
        this.updatedAt = now;
        return true;
    }

    /** Puts the game at that place in quick access. The QuickAccess rules decide the position. */
    public void pinAt(int position, Instant now) {
        if (position < 1) {
            throw new IllegalArgumentException("pin positions start at 1");
        }
        this.pinPosition = position;
        this.updatedAt = now;
    }

    public void unpin(Instant now) {
        if (pinPosition != null) {
            this.pinPosition = null;
            this.updatedAt = now;
        }
    }

    // ------------------------------------------------------------------ reading

    public long userId() {
        return userId;
    }

    public long gameId() {
        return gameId;
    }

    public TrackingStatus status() {
        return status;
    }

    /** null when the game is not in quick access. */
    public Integer pinPosition() {
        return pinPosition;
    }

    public boolean pinned() {
        return pinPosition != null;
    }

    public Instant addedAt() {
        return addedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof TrackedGame t && userId == t.userId && gameId == t.gameId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, gameId);
    }

    @Override
    public String toString() {
        return "TrackedGame{userId=" + userId + ", gameId=" + gameId + ", status=" + status
                + ", pinPosition=" + pinPosition + "}";
    }
}
