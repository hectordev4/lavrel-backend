package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformGameInfo;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.StoreItem;
import com.lavrel.catalog.domain.ContentPolicy;
import com.lavrel.catalog.domain.GameSignals;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Decides whether a Steam app may enter the catalog. No Steam calls and no writes: it only judges the data it is
 * given, and returns the reason when the app must stay out. The reasons are remembered as import failures.
 *  - cheap check: the store item fetched 25 at a time already has the type, content descriptors and tag
 *    weights, so every content rule runs here before any per-game call is spent;
 *  - full check: again with the store page, which adds its genres and its own descriptor list.
 */
@Component
class SteamGameScreening {

    static final String NO_ACHIEVEMENTS = "no achievements";
    static final String TOO_FEW_REVIEWS = "too few reviews";
    static final String NOT_FOUND = "not found in the Steam store";

    private final ContentPolicy contentPolicy;
    private final int minReviews;

    SteamGameScreening(ContentPolicy contentPolicy, @Value("${app.catalog.min-reviews:50}") int minReviews) {
        this.contentPolicy = contentPolicy;
        this.minReviews = minReviews;
    }

    /** @param popularityRequired also reject games with fewer than app.catalog.min-reviews reviews */
    Optional<String> cheapRejection(String appId, StoreItem item, boolean popularityRequired) {
        if (item == null || !item.found()) {
            return Optional.of(NOT_FOUND);
        }
        if (!"game".equals(item.type())) {
            return Optional.of("type: " + item.type());
        }
        Optional<String> policy = contentPolicy.evaluate(new GameSignals(appIdOf(appId), item.type(),
                item.contentDescriptorIds(), item.tags(), List.of()));
        if (policy.isPresent()) {
            return policy;
        }
        if (popularityRequired && minReviews > 0
                && (item.reviewCount() == null || item.reviewCount() < minReviews)) {
            return Optional.of(TOO_FEW_REVIEWS);
        }
        return Optional.empty();
    }

    Optional<String> fullRejection(String appId, PlatformGameInfo info, StoreItem item) {
        if (info.type() != null && !"game".equals(info.type())) {
            return Optional.of("type: " + info.type());
        }
        return contentPolicy.evaluate(new GameSignals(appIdOf(appId), info.type(),
                info.contentDescriptorIds(),
                item == null ? List.of() : item.tags(),
                new ArrayList<>(info.genres().values())));
    }

    /** Steam app ids are numeric; the id is only used in the policy's reasons, so 0 is a safe fallback. */
    private static long appIdOf(String steamAppId) {
        try {
            return Long.parseLong(steamAppId);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
