package com.lavrel.catalog.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A canonical game, copied from Steam. description may be empty when the platform has none; null only for games
 * stored before descriptions were copied.
 *
 * An entity: it has an id and it changes over time, when its data is copied again from the platform
 * ({@link #refreshFromPlatform}). Create it with {@link #create}; the persistence adapter rebuilds stored ones with
 * {@link #restore}. The details shown on the game page (tags, creators, features...) are kept apart, in GameMetadata.
 */
public class Game {

    private final Long id;
    private String title;
    private String coverImageUrl;
    private String description;
    private String storeUrl;
    private final Instant createdAt;

    private Game(Long id, String title, String coverImageUrl, String description, String storeUrl,
                 Instant createdAt) {
        this.id = id;
        this.title = Objects.requireNonNull(title, "title");
        this.coverImageUrl = coverImageUrl;
        this.description = description;
        this.storeUrl = storeUrl;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** A game seen for the first time: no id yet. A missing description becomes empty ("fetched, none"). */
    public static Game create(String title, String coverImageUrl, String description, String storeUrl, Instant now) {
        return new Game(null, title, coverImageUrl, emptyIfNull(description), storeUrl, now);
    }

    /** Rebuilds a stored game. For persistence adapters only. */
    public static Game restore(Long id, String title, String coverImageUrl, String description, String storeUrl,
                               Instant createdAt) {
        return new Game(id, title, coverImageUrl, description, storeUrl, createdAt);
    }

    // ------------------------------------------------------------------ behaviour

    /** Replaces the copied data with what the platform says now. The id and the creation time stay. */
    public void refreshFromPlatform(String newTitle, String newCoverImageUrl, String newDescription,
                                    String newStoreUrl) {
        this.title = Objects.requireNonNull(newTitle, "title");
        this.coverImageUrl = newCoverImageUrl;
        this.description = emptyIfNull(newDescription);
        this.storeUrl = newStoreUrl;
    }

    // ------------------------------------------------------------------ reading

    /** null until the game is stored. */
    public Long id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String coverImageUrl() {
        return coverImageUrl;
    }

    public String description() {
        return description;
    }

    public String storeUrl() {
        return storeUrl;
    }

    public Instant createdAt() {
        return createdAt;
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    /** Stored games are equal when they have the same id; a game that is not stored yet equals only itself. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Game game && id != null && id.equals(game.id);
    }

    /** Constant on purpose: the id appears only after the game is stored, and the hash must not change meanwhile. */
    @Override
    public int hashCode() {
        return Game.class.hashCode();
    }

    @Override
    public String toString() {
        return "Game{id=" + id + ", title=" + title + "}";
    }
}
