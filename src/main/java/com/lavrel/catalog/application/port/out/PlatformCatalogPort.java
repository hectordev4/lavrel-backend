package com.lavrel.catalog.application.port.out;

import com.lavrel.catalog.domain.GameMetadata.AgeRating;
import com.lavrel.catalog.domain.GameMetadata.Creator;
import com.lavrel.catalog.domain.GameMetadata.TagWeight;
import com.lavrel.shared.domain.Platform;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Implemented by the platform clients (SteamClient, XboxClient, PsnClient). */
public interface PlatformCatalogPort {

    Platform platform();

    /** The platform's full store page data for one game (one call per game). */
    Optional<PlatformGameInfo> fetchGameInfo(String platformGameId);

    /**
     * Lightweight data for many games in ONE call (tags with weights, creators, reviews, categories...).
     * Games the platform does not know are missing from the result.
     */
    Map<String, StoreItem> fetchStoreItems(List<String> platformGameIds);

    /** Tag id -> name, for every tag the platform has. */
    Map<Long, String> fetchTagNames();

    List<PlatformAchievement> fetchAchievements(String platformGameId);

    /** Ids of the most played games on the platform right now, most played first. */
    List<String> fetchMostPlayedGameIds(int limit);

    /**
     * One page of the platform's full game list, in ascending id order, starting after afterId.
     * Pass the returned lastId as afterId to continue.
     */
    GameIdPage fetchGameIdPage(long afterId, int pageSize);

    /**
     * type is the platform's classification ("game", "dlc", "music"...). categories maps category id to its
     * display name. contentDescriptorIds are what the developer declared; genres maps Steam's genre id to its name.
     */
    record PlatformGameInfo(String platformGameId, String title, String coverImageUrl, String description,
                            String type, List<Integer> contentDescriptorIds, Map<Integer, String> genres,
                            Map<Integer, String> categories, List<AgeRating> ageRatings,
                            boolean free, Integer requiredAge, boolean windows, boolean mac, boolean linux,
                            List<String> developers, List<String> publishers) {
    }

    /** found is false when the platform does not know the game or hides it. */
    record StoreItem(String platformGameId, boolean found, String type, String name, String shortDescription,
                     List<TagWeight> tags, List<Integer> contentDescriptorIds,
                     List<Integer> playerCategoryIds, List<Integer> featureCategoryIds,
                     List<Integer> controllerCategoryIds,
                     Integer reviewCount, Integer reviewPercentPositive, Long releaseEpochSeconds,
                     boolean windows, boolean mac, boolean linux, String esrbRating, Integer requiredAge,
                     List<Creator> developers, List<Creator> publishers, List<Creator> franchises) {
    }

    record PlatformAchievement(String platformAchievementId, String name, String description,
                               String iconUrl, boolean hidden) {
    }

    record GameIdPage(List<String> ids, long lastId, boolean hasMore) {
    }
}
