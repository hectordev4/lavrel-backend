package com.lavrel.catalog.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the game page shows besides title and description. Stored in its own tables
 * (see migration V9); a game is published only once its metadata exists.
 */
public record GameMetadata(
        LocalDate releaseDate,
        boolean windows,
        boolean mac,
        boolean linux,
        Integer reviewCount,
        Integer reviewPercentPositive,
        boolean free,
        Integer requiredAge,
        List<Creator> developers,
        List<Creator> publishers,
        List<Creator> franchises,
        List<TagWeight> tags,
        List<Genre> genres,
        List<Feature> features,
        List<AgeRating> ageRatings) {

    public GameMetadata {
        developers = developers == null ? List.of() : List.copyOf(developers);
        publishers = publishers == null ? List.of() : List.copyOf(publishers);
        franchises = franchises == null ? List.of() : List.copyOf(franchises);
        tags = tags == null ? List.of() : List.copyOf(tags);
        genres = genres == null ? List.of() : List.copyOf(genres);
        features = features == null ? List.of() : List.copyOf(features);
        ageRatings = ageRatings == null ? List.of() : List.copyOf(ageRatings);
    }

    public enum FeatureGroup {
        PLAYER_MODE, FEATURE, ACCESSIBILITY, CONTROLLER
    }

    /** A developer, publisher or franchise. steamCreatorId is Steam's id for it, when known. */
    public record Creator(String name, Long steamCreatorId) {
    }

    /** name is null until the tag name list has been loaded (it is filled in when stored or read). */
    public record TagWeight(long tagId, String name, int weight) {
    }

    /** One of Steam's curated genres (Action, RPG, Indie...). */
    public record Genre(int id, String name) {
    }

    /** A Steam store category. name may be null when only its id is known yet. */
    public record Feature(int id, String name, FeatureGroup group) {
    }

    public record AgeRating(String board, String rating, List<String> descriptors) {
        public AgeRating {
            descriptors = descriptors == null ? List.of() : List.copyOf(descriptors);
        }
    }
}
