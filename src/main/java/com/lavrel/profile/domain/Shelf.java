package com.lavrel.profile.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One shelf of a user's public profile. collectionId is set only for COLLECTION shelves; achievementIds only means
 * something for ACHIEVEMENTS shelves (in the order the user chose). The API uses publicId.
 *
 * An entity: it changes over time (retitle, move, pick achievements). Create it with {@link #create}; persistence
 * adapters rebuild stored ones with {@link #restore}.
 */
public class Shelf {

    public static final int MAX_ACHIEVEMENTS = 12;

    private final Long id;
    private final UUID publicId;
    private final long userId;
    private final ShelfType type;
    private String title;
    private int position;
    private final UUID collectionId;
    private List<Long> achievementIds;
    private final Instant createdAt;

    private Shelf(Long id, UUID publicId, long userId, ShelfType type, String title, int position, UUID collectionId,
                  List<Long> achievementIds, Instant createdAt) {
        if ((type == ShelfType.COLLECTION) != (collectionId != null)) {
            throw new InvalidShelfException("A collection shelf needs a collection, and only it can have one");
        }
        if (position < 1) {
            throw new IllegalArgumentException("positions start at 1");
        }
        this.id = id;
        this.publicId = Objects.requireNonNull(publicId, "publicId");
        this.userId = userId;
        this.type = Objects.requireNonNull(type, "type");
        this.title = title;
        this.position = position;
        this.collectionId = collectionId;
        this.achievementIds = List.copyOf(achievementIds);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** title must already be validated (ShelfTitles); null means the frontend shows the type's own name. */
    public static Shelf create(long userId, ShelfType type, String title, int position, UUID collectionId,
                               Instant now) {
        return new Shelf(null, UUID.randomUUID(), userId, type, title, position, collectionId, List.of(), now);
    }

    public static Shelf restore(Long id, UUID publicId, long userId, ShelfType type, String title, int position,
                                UUID collectionId, List<Long> achievementIds, Instant createdAt) {
        return new Shelf(id, publicId, userId, type, title, position, collectionId, achievementIds, createdAt);
    }

    public void retitle(String validTitle) {
        this.title = validTitle;
    }

    /** @return true if the position changed */
    public boolean moveTo(int newPosition) {
        if (newPosition < 1) {
            throw new IllegalArgumentException("positions start at 1");
        }
        boolean changed = position != newPosition;
        position = newPosition;
        return changed;
    }

    /**
     * The achievements to show, in order. The caller checks that the user unlocked them.
     * @throws InvalidShelfException if this is not an achievement shelf, or the list is too long or repeats one
     */
    public void showAchievements(List<Long> ids) {
        if (type != ShelfType.ACHIEVEMENTS) {
            throw new InvalidShelfException("Only an achievement shelf shows picked achievements");
        }
        if (ids.size() > MAX_ACHIEVEMENTS) {
            throw new InvalidShelfException("An achievement shelf shows at most " + MAX_ACHIEVEMENTS);
        }
        if (ids.stream().distinct().count() != ids.size()) {
            throw new InvalidShelfException("An achievement appears more than once");
        }
        this.achievementIds = List.copyOf(ids);
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

    public ShelfType type() {
        return type;
    }

    public String title() {
        return title;
    }

    public int position() {
        return position;
    }

    public UUID collectionId() {
        return collectionId;
    }

    public List<Long> achievementIds() {
        return achievementIds;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Shelf s && publicId.equals(s.publicId);
    }

    @Override
    public int hashCode() {
        return publicId.hashCode();
    }

    @Override
    public String toString() {
        return "Shelf{publicId=" + publicId + ", type=" + type + ", position=" + position + "}";
    }
}
