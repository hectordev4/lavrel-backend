package com.lavrel.assistant.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A saved answer to "how do I get this achievement?", in one language. It is the same for every player (it never
 * contains anyone's progress), so it is generated once and reused. grounded: written from wiki text that allows reuse
 * (sources lists them); otherwise from the model's own knowledge, which can be wrong, so it expires sooner.
 * curated: written or rewritten by an admin; it never expires and is never replaced automatically.
 */
public record Guide(long achievementId, String language, String text, boolean grounded, List<Source> sources,
                    String model, Instant createdAt, boolean curated) {

    public static final Duration GROUNDED_LIFETIME = Duration.ofDays(30);
    public static final Duration UNGROUNDED_LIFETIME = Duration.ofDays(7);

    public Guide {
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(text, "text");
        sources = sources == null ? List.of() : List.copyOf(sources);
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public boolean freshAt(Instant now) {
        return curated || createdAt.plus(grounded ? GROUNDED_LIFETIME : UNGROUNDED_LIFETIME).isAfter(now);
    }

    /**
     * Where the information came from, shown under the answer as the credit CC BY-SA asks for: the page and its link,
     * the wiki's own name and its license (with the license's link). Guides saved before 0.17.0 have no site or
     * licenseUrl (null).
     */
    public record Source(String title, String url, String site, String license, String licenseUrl) {

        /** Without the wiki's name or license link. */
        public Source(String title, String url, String license) {
            this(title, url, null, license, null);
        }
    }
}
