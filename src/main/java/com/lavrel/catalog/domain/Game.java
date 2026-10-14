package com.lavrel.catalog.domain;

import java.time.Instant;
import java.util.Objects;

/** APP. Canonical game, initially seeded from Steam. */
public record Game(Long id, String title, String coverImageUrl, Instant createdAt) {
    public Game {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
