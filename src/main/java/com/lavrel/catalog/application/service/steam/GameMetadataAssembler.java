package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformGameInfo;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.StoreItem;
import com.lavrel.catalog.domain.GameFeature;
import com.lavrel.catalog.domain.GameMetadata;
import com.lavrel.catalog.domain.GameMetadata.Creator;
import com.lavrel.catalog.domain.GameMetadata.Feature;
import com.lavrel.catalog.domain.GameMetadata.FeatureGroup;
import com.lavrel.catalog.domain.GameMetadata.Genre;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the two kinds of platform data (the cheap store item and the full store page) into the GameMetadata we
 * store. Pure functions with no I/O, so it can be tested on its own; the importer only orchestrates.
 * The store item wins where both have a value, because it is the richer source.
 */
final class GameMetadataAssembler {

    private GameMetadataAssembler() {
    }

    static GameMetadata assemble(PlatformGameInfo info, StoreItem item) {
        LocalDate release = null;
        if (item != null && item.releaseEpochSeconds() != null && item.releaseEpochSeconds() > 0) {
            release = Instant.ofEpochSecond(item.releaseEpochSeconds()).atZone(ZoneOffset.UTC).toLocalDate();
        }
        List<Creator> developers = item != null && !item.developers().isEmpty()
                ? item.developers() : info.developers().stream().map(name -> new Creator(name, null)).toList();
        List<Creator> publishers = item != null && !item.publishers().isEmpty()
                ? item.publishers() : info.publishers().stream().map(name -> new Creator(name, null)).toList();

        boolean windows = item != null ? item.windows() : info.windows();
        boolean mac = item != null ? item.mac() : info.mac();
        boolean linux = item != null ? item.linux() : info.linux();
        Integer requiredAge = item != null && item.requiredAge() != null ? item.requiredAge() : info.requiredAge();

        return new GameMetadata(release, windows, mac, linux,
                item == null ? null : item.reviewCount(),
                item == null ? null : item.reviewPercentPositive(),
                info.free(), requiredAge, developers, publishers,
                item == null ? List.of() : item.franchises(),
                item == null ? List.of() : item.tags(),
                info.genres().entrySet().stream().map(e -> new Genre(e.getKey(), e.getValue())).toList(),
                buildFeatures(info, item), info.ageRatings());
    }

    /**
     * The store item says which group each category belongs to; the store page gives their names.
     * Categories only the store page knows are added as plain features.
     */
    private static List<Feature> buildFeatures(PlatformGameInfo info, StoreItem item) {
        Map<Integer, Feature> byId = new LinkedHashMap<>();
        if (item != null) {
            addFeatures(byId, item.playerCategoryIds(), FeatureGroup.PLAYER_MODE, info);
            addFeatures(byId, item.featureCategoryIds(), FeatureGroup.FEATURE, info);
            addFeatures(byId, item.controllerCategoryIds(), FeatureGroup.CONTROLLER, info);
        }
        info.categories().forEach((id, name) ->
                byId.putIfAbsent(id, new Feature(id, name, groupOf(id, FeatureGroup.FEATURE))));
        return List.copyOf(byId.values());
    }

    private static void addFeatures(Map<Integer, Feature> byId, List<Integer> ids, FeatureGroup listGroup,
                                    PlatformGameInfo info) {
        for (Integer id : ids) {
            byId.putIfAbsent(id, new Feature(id, info.categories().get(id), groupOf(id, listGroup)));
        }
    }

    /** Known features keep their own group (accessibility items arrive inside the "features" list). */
    private static FeatureGroup groupOf(int categoryId, FeatureGroup fallback) {
        return GameFeature.byCategoryId(categoryId).map(GameFeature::group).orElse(fallback);
    }
}
