package com.lavrel.catalog.service;

import com.lavrel.catalog.domain.ContentPolicy;
import com.lavrel.catalog.domain.GameSignals;
import com.lavrel.catalog.domain.GameMetadata.TagWeight;
import com.lavrel.catalog.steam.SteamAppDetailsClient;
import com.lavrel.catalog.steam.SteamAppDetailsClient.AppDetails;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class CatalogFilterService {

    /**
     * kept: passed every layer. rejected: appId to reason (safe to remember permanently).
     * unavailable: Steam failed or said success=false; never remembered as a rejection, retry later.
     */
    public record FilterResult(List<Long> kept, Map<Long, String> rejected, List<Long> unavailable) {
    }

    private static final Logger log = LoggerFactory.getLogger(CatalogFilterService.class);

    private final SteamAppDetailsClient steam;
    private final ContentPolicy policy;
    private final Duration pause;

    public CatalogFilterService(SteamAppDetailsClient steam, ContentPolicy policy,
            @Value("${app.catalog.steam-pause:1500ms}") Duration pause) {
        this.steam = steam;
        this.policy = policy;
        this.pause = pause;
    }

    /**
     * @param appIds    candidates, in the order to check them
     * @param tagsByApp tag weights from the batched store call we already make (no extra Steam cost)
     */
    public FilterResult filter(List<Long> appIds, Map<Long, List<TagWeight>> tagsByApp) {
        List<Long> kept = new ArrayList<>();
        Map<Long, String> rejected = new LinkedHashMap<>();
        List<Long> unavailable = new ArrayList<>();

        for (long appId : appIds) {
            List<TagWeight> tags = tagsByApp.getOrDefault(appId, List.of());

            // Pre-screen on tags alone: Layers 2 and 3 reject without spending an appdetails call.
            Optional<String> early = policy.evaluate(new GameSignals(appId, null, List.of(), tags, List.of()));
            if (early.isPresent()) {
                rejected.put(appId, early.get());
                continue;
            }

            Optional<AppDetails> details;
            try {
                details = steam.fetch(appId);
            } catch (RestClientException e) {
                log.warn("appdetails failed for {}: {}", appId, e.getMessage());
                unavailable.add(appId);
                continue;
            } finally {
                if (!pauseBetweenCalls()) {
                    break;
                }
            }
            if (details.isEmpty()) {
                unavailable.add(appId);
                continue;
            }

            AppDetails d = details.get();
            GameSignals full = new GameSignals(appId, d.type(), d.contentDescriptorIds(), tags, d.genres());
            policy.evaluate(full).ifPresentOrElse(
                    reason -> rejected.put(appId, reason),
                    () -> kept.add(appId));
        }
        return new FilterResult(kept, rejected, unavailable);
    }

    private boolean pauseBetweenCalls() {
        try {
            Thread.sleep(pause.toMillis());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
