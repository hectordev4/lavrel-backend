package com.lavrel.catalog.config;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Every threshold and list of the filter, overridable under app.catalog.filter.* without code changes. */
@ConfigurationProperties("app.catalog.filter")
public record CatalogFilterProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("game") String requiredType,
        // Layer 1: immediate reject (3 = Adult Only Sexual Content)
        @DefaultValue("3") Set<Integer> rejectedDescriptorIds,
        // Layer 1b: descriptors that reject only when a sexual tag is in the top ranks (4 = Frequent Nudity or Sexual Content)
        @DefaultValue("4") Set<Integer> strictDescriptorIds,
        @DefaultValue("3") int strictDescriptorMaxRank,
        // Layer 2: Sexual Content, Nudity
        @DefaultValue({"12095", "6650"}) Set<Long> sexualTagIds,
        @DefaultValue("5") int normalizationCount,
        @DefaultValue("0.20") double rejectShare,
        @DefaultValue("3") int rejectMaxRank,
        // Layer 3: Hentai / adult (9130, 6653). Visual Novel (3799) and Match 3 (1665) are allowed on purpose.
        @DefaultValue({"9130", "6653"}) Set<Long> excludedTagIds,
        @DefaultValue("Hentai") Set<String> excludedGenres) {

    /** Same values as the @DefaultValue annotations, for tests that build a policy without Spring. */
    public static CatalogFilterProperties defaults(boolean enabled) {
        return new CatalogFilterProperties(enabled, "game", Set.of(3), Set.of(4), 3,
                Set.of(12095L, 6650L), 5, 0.20, 3,
                Set.of(9130L, 6653L), Set.of("Hentai"));
    }
}
