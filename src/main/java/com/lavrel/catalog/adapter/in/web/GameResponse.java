package com.lavrel.catalog.adapter.in.web;

import com.lavrel.catalog.application.port.in.GetGameUseCase.GameDetails;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The JSON of GET /api/games/{id}. Kept separate from the use case's read model so the API shape can change
 * without touching the application layer (and the other way round).
 * slug is the readable part of the page address: the frontend builds /games/{id}-{slug}; only the id matters.
 */
public record GameResponse(long id, String title, String slug, String coverImageUrl, String description,
                           String storeUrl, LocalDate releaseDate, Platforms platforms, Reviews reviews,
                           boolean free, Integer requiredAge, List<String> developers, List<String> publishers,
                           List<String> franchises, List<Tag> tags, List<Genre> genres,
                           Map<String, Boolean> features, int accessibilityFeatureCount,
                           List<FeatureInfo> allFeatures, List<AgeRating> ageRatings) {

    public static GameResponse from(GameDetails d) {
        return new GameResponse(d.id(), d.title(), d.slug(), d.coverImageUrl(), d.description(), d.storeUrl(),
                d.releaseDate(),
                new Platforms(d.platforms().windows(), d.platforms().mac(), d.platforms().linux()),
                new Reviews(d.reviews().count(), d.reviews().percentPositive()),
                d.free(), d.requiredAge(), d.developers(), d.publishers(), d.franchises(),
                d.tags().stream().map(t -> new Tag(t.id(), t.name(), t.weight())).toList(),
                d.genres().stream().map(g -> new Genre(g.id(), g.name())).toList(),
                d.features(), d.accessibilityFeatureCount(),
                d.allFeatures().stream().map(f -> new FeatureInfo(f.id(), f.name(), f.group())).toList(),
                d.ageRatings().stream().map(r -> new AgeRating(r.board(), r.rating(), r.descriptors())).toList());
    }

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

    public record AgeRating(String board, String rating, List<String> descriptors) {
    }
}
