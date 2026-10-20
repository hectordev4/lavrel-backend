package com.lavrel.catalog.application.port.in;

import com.lavrel.catalog.domain.GameNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface GetGameUseCase {

    /** @throws GameNotFoundException if the game does not exist or is not published yet */
    GameDetails getGame(long gameId);

    /**
     * What the game page shows. This read model is also the shape of the API response.
     * features holds one boolean per feature the frontend can show or hide; allFeatures lists every Steam
     * category the game has, with its group (PLAYER_MODE, FEATURE, ACCESSIBILITY, CONTROLLER).
     * slug is the readable part of the page address: the frontend builds /games/{id}-{slug}; only the id matters.
     * Fields that Steam does not provide are null; description is null when empty.
     */
    record GameDetails(long id, String title, String slug, String coverImageUrl, String description, String storeUrl,
                       LocalDate releaseDate, Platforms platforms, Reviews reviews, boolean free,
                       Integer requiredAge, List<String> developers, List<String> publishers,
                       List<String> franchises, List<Tag> tags, List<Genre> genres, Map<String, Boolean> features,
                       int accessibilityFeatureCount, List<FeatureInfo> allFeatures,
                       List<AgeRatingInfo> ageRatings) {

        public record Platforms(boolean windows, boolean mac, boolean linux) {
        }

        public record Reviews(Integer count, Integer percentPositive) {
        }

        public record Tag(long id, String name, int weight) {
        }

        public record Genre(int id, String name) {
        }

        public record FeatureInfo(int id, String name, String group) {
        }

        public record AgeRatingInfo(String board, String rating, List<String> descriptors) {
        }
    }
}
