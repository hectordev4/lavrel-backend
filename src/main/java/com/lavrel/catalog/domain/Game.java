package com.lavrel.catalog.domain;

import java.time.Instant;
import java.util.Objects;

/** APP. Canonical game, initially seeded from Steam. description may be empty when the platform has none. */
public record Game(
        Long id,
        String title,
        String coverImageUrl,
        String description,
        String storeUrl,
        Instant createdAt) {

    public Game {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
