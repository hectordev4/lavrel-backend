package com.lavrel.assistant.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A saved answer to "how do I get this achievement?", in one language. It is the same for every player (it never
 * contains anyone's progress), so it is generated once and reused. grounded: written from wiki text that allows reuse
 * (sources lists them); otherwise from the model's own knowledge, which can be wrong, so it expires sooner.
 */
public record Guide(long achievementId, String language, String text, boolean grounded, List<Source> sources,
                    String model, Instant createdAt) {

    public static final Duration GROUNDED_LIFETIME = Duration.ofDays(30);
    public static final Duration UNGROUNDED_LIFETIME = Duration.ofDays(7);

    public Guide {
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(text, "text");
        sources = sources == null ? List.of() : List.copyOf(sources);
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public boolean freshAt(Instant now) {
        return createdAt.plus(grounded ? GROUNDED_LIFETIME : UNGROUNDED_LIFETIME).isAfter(now);
    }

    /** Where the information came from, with its license, shown under the answer. */
    public record Source(String title, String url, String license) {
    }
}
