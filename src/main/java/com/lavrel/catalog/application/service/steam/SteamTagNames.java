package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.out.GameMetadataRepositoryPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.domain.GameMetadata.TagWeight;
import com.lavrel.shared.domain.PlatformUnavailableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Keeps Steam's tag names in our database. A single shared bean on purpose: it remembers when it last asked
 * Steam, so every import path together asks at most once an hour.
 */
@Component
class SteamTagNames {

    private static final Logger log = LoggerFactory.getLogger(SteamTagNames.class);
    private static final Duration MIN_INTERVAL = Duration.ofHours(1);

    private final PlatformCatalogPort steam;
    private final GameMetadataRepositoryPort metadata;
    private final Clock clock;

    private volatile Instant refreshedAt = Instant.EPOCH;

    SteamTagNames(List<PlatformCatalogPort> catalogPorts, GameMetadataRepositoryPort metadata, Clock clock) {
        this.steam = SteamCatalogPorts.steam(catalogPorts);
        this.metadata = metadata;
        this.clock = clock;
    }

    /**
     * Loads Steam's tag names when a game has a tag we have no name for (at most once an hour). Only fills
     * placeholders, never renames; if Steam fails, placeholders are used meanwhile.
     */
    void ensureNames(List<TagWeight> tags) {
        Set<Long> ids = tags.stream().map(TagWeight::tagId).collect(Collectors.toSet());
        if (ids.isEmpty() || metadata.findKnownTagIds(ids).containsAll(ids)) {
            return;
        }
        Instant now = clock.instant();
        if (Duration.between(refreshedAt, now).compareTo(MIN_INTERVAL) < 0) {
            return;
        }
        refreshedAt = now;
        try {
            metadata.saveTagNames(steam.fetchTagNames(), false);
        } catch (PlatformUnavailableException e) {
            log.warn("Could not load Steam's tag names: {}", e.getMessage());
        }
    }

    /** Replaces every stored tag name with Steam's current one (manual refresh). */
    void replaceAll() {
        try {
            metadata.saveTagNames(steam.fetchTagNames(), true);
            refreshedAt = clock.instant();
        } catch (PlatformUnavailableException e) {
            log.warn("Could not refresh Steam's tag names: {}", e.getMessage());
        }
    }
}
