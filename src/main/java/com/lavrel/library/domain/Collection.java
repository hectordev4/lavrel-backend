package com.lavrel.library.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A named list of the user's own games ("Platinum hunt", "Cozy games"). Only owned games go in (the use case
 * checks), and a game can be in several collections. publicId is what the API uses; the database id stays inside.
 *
 * An entity: it changes over time (rename, move). Create it with {@link #create}; persistence adapters rebuild stored
 * ones with {@link #restore}.
 */
public class Collection {

    private final Long id;
    private final UUID publicId;
    private final long userId;
    private String name;
    private int position;
    private final Instant createdAt;

    private Collection(Long id, UUID publicId, long userId, String name, int position, Instant createdAt) {
        if (position < 1) {
            throw new IllegalArgumentException("positions start at 1");
        }
        this.id = id;
        this.publicId = Objects.requireNonNull(publicId, "publicId");
        this.userId = userId;
        this.name = Objects.requireNonNull(name, "name");
        this.position = position;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** A new collection with an already validated name (CollectionNames), placed at the end. */
    public static Collection create(long userId, String validName, int position, Instant now) {
        return new Collection(null, UUID.randomUUID(), userId, validName, position, now);
    }

    public static Collection restore(Long id, UUID publicId, long userId, String name, int position,
                                     Instant createdAt) {
        return new Collection(id, publicId, userId, name, position, createdAt);
    }

    /** @return true if the name changed */
    public boolean rename(String validName) {
        if (name.equals(validName)) {
            return false;
        }
        this.name = Objects.requireNonNull(validName, "name");
        return true;
    }

    /** @return true if the position changed */
    public boolean moveTo(int newPosition) {
        if (newPosition < 1) {
            throw new IllegalArgumentException("positions start at 1");
        }
        if (position == newPosition) {
            return false;
        }
        this.position = newPosition;
        return true;
    }

    public boolean belongsTo(long someUserId) {
        return userId == someUserId;
    }

    public Long id() {
        return id;
    }

    public UUID publicId() {
        return publicId;
    }

    public long userId() {
        return userId;
    }

    public String name() {
        return name;
    }

    public int position() {
        return position;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Collection c && publicId.equals(c.publicId);
    }

    @Override
    public int hashCode() {
        return publicId.hashCode();
    }

    @Override
    public String toString() {
        return "Collection{publicId=" + publicId + ", userId=" + userId + ", name=" + name + ", position=" + position
                + "}";
    }
}
