package com.lavrel.shared.web;

import java.time.Duration;
import org.springframework.http.CacheControl;

/**
 * How long browsers may keep public catalog answers. The catalog only changes through admin tools (seed, refresh,
 * exceptions), so short caches are safe and spare the free database repeated identical queries.
 */
public final class PublicCache {

    /** Game pages and achievement lists seen by anonymous visitors. */
    public static final CacheControl GAME = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    /** Search results and the type-ahead. */
    public static final CacheControl SEARCH = CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic();

    /** Tag and creator options for the filters. */
    public static final CacheControl FILTER_OPTIONS = CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic();

    /** Anything with the caller's own data. */
    public static final CacheControl PRIVATE = CacheControl.noCache().cachePrivate();

    private PublicCache() {
    }
}
