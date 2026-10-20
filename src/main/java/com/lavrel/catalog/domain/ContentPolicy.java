package com.lavrel.catalog.domain;

import com.lavrel.catalog.domain.GameMetadata.TagWeight;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decides whether a game may enter the catalog. The catalog is shown to visitors who are not logged in,
 * so it must be safe for work, while mainstream games with some nudity (The Witcher 3...) should stay.
 *
 * The decisive signal is the user tags WITH their weights: an adult game has "Sexual Content" or "Nudity"
 * among its top tags, a mainstream game has them far down the list or with a small share of the top weight.
 *
 * STANDARD (default) blocks:
 *  - content descriptors "Adult Only Sexual Content" (3) and "Frequent Nudity or Sexual Content" (4);
 *  - ESRB "Adults Only";
 *  - the tag "Hentai", at any weight;
 *  - the tags "Sexual Content", "Nudity" and "Sexual Themes" when prominent: within the top {topRank}
 *    tags, or with at least {weightRatio} of the top tag's weight;
 *  - the store genres "Sexual Content" and "Nudity";
 *  - explicit wording in the title or short description.
 * STRICT also blocks "Some Nudity or Sexual Content" (1) and those three tags at any weight.
 * OFF allows everything.
 */
public final class ContentPolicy {

    public enum Level {
        OFF, STANDARD, STRICT;

        /** Missing means the default (STANDARD); an unknown value falls back to the safest level. */
        public static Level parse(String value) {
            if (value == null || value.isBlank()) {
                return STANDARD;
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "off" -> OFF;
                case "standard" -> STANDARD;
                default -> STRICT;
            };
        }
    }

    /** What is known about a game when the policy is applied; anything may be null or empty. */
    public record Signals(String title, String description, Collection<Integer> contentDescriptorIds,
                          Collection<String> genres, List<TagWeight> tags, String esrbRating) {
    }

    private static final int SOME_NUDITY_OR_SEXUAL = 1;
    private static final int ADULT_ONLY_SEXUAL = 3;
    private static final int FREQUENT_NUDITY_OR_SEXUAL = 4;

    // Steam's tag ids (from the store's tag list)
    private static final long TAG_HENTAI = 9130;
    private static final long TAG_SEXUAL_CONTENT = 12095;
    private static final long TAG_NUDITY = 6650;
    private static final long TAG_SEXUAL_THEMES = 40500;
    private static final Set<Long> SEXUAL_TAGS = Set.of(TAG_SEXUAL_CONTENT, TAG_NUDITY, TAG_SEXUAL_THEMES);

    private static final Set<String> SEXUAL_GENRES = Set.of("sexual content", "nudity");

    private static final Pattern EXPLICIT_WORDS = Pattern.compile(
            "\\b(hentai|porn\\w*|nsfw|erotic\\w*|eroge|ecchi|lewd|xxx)\\b", Pattern.CASE_INSENSITIVE);

    private final Level level;
    private final int sexualTagTopRank;
    private final double sexualTagWeightRatio;

    public ContentPolicy(Level level, int sexualTagTopRank, double sexualTagWeightRatio) {
        this.level = level;
        this.sexualTagTopRank = sexualTagTopRank;
        this.sexualTagWeightRatio = sexualTagWeightRatio;
    }

    /** @return why the game must stay out of the catalog, or empty if it is allowed */
    public Optional<String> rejectionReason(Signals signals) {
        if (level == Level.OFF) {
            return Optional.empty();
        }
        Set<Integer> descriptors = signals.contentDescriptorIds() == null
                ? Set.of() : new HashSet<>(signals.contentDescriptorIds());

        if (descriptors.contains(ADULT_ONLY_SEXUAL)) {
            return Optional.of("adult content: Adult Only Sexual Content");
        }
        if (descriptors.contains(FREQUENT_NUDITY_OR_SEXUAL)) {
            return Optional.of("adult content: Frequent Nudity or Sexual Content");
        }
        if ("ao".equalsIgnoreCase(signals.esrbRating())) {
            return Optional.of("adult content: ESRB Adults Only");
        }

        Optional<String> byTags = rejectionByTags(signals.tags());
        if (byTags.isPresent()) {
            return byTags;
        }

        if (signals.genres() != null) {
            for (String genre : signals.genres()) {
                if (genre != null && SEXUAL_GENRES.contains(genre.trim().toLowerCase(Locale.ROOT))) {
                    return Optional.of("adult content: genre " + genre.trim());
                }
            }
        }
        if (containsExplicitWording(signals.title()) || containsExplicitWording(signals.description())) {
            return Optional.of("adult content: explicit wording");
        }
        if (level == Level.STRICT && descriptors.contains(SOME_NUDITY_OR_SEXUAL)) {
            return Optional.of("adult content: Some Nudity or Sexual Content");
        }
        return Optional.empty();
    }

    private Optional<String> rejectionByTags(List<TagWeight> tags) {
        if (tags == null || tags.isEmpty()) {
            return Optional.empty();
        }
        List<TagWeight> ranked = tags.stream()
                .sorted(Comparator.comparingInt(TagWeight::weight).reversed())
                .toList();
        long topWeight = ranked.get(0).weight();

        for (int rank = 0; rank < ranked.size(); rank++) {
            TagWeight tag = ranked.get(rank);
            if (tag.tagId() == TAG_HENTAI) {
                return Optional.of("adult content: tag Hentai");
            }
            if (SEXUAL_TAGS.contains(tag.tagId())) {
                boolean prominent = rank < sexualTagTopRank
                        || (topWeight > 0 && tag.weight() >= sexualTagWeightRatio * topWeight);
                if (level == Level.STRICT || prominent) {
                    return Optional.of("adult content: tag " + sexualTagLabel(tag.tagId())
                            + " (rank " + (rank + 1) + ", weight " + tag.weight() + " of " + topWeight + ")");
                }
            }
        }
        return Optional.empty();
    }

    private static String sexualTagLabel(long tagId) {
        if (tagId == TAG_NUDITY) {
            return "Nudity";
        }
        return tagId == TAG_SEXUAL_THEMES ? "Sexual Themes" : "Sexual Content";
    }

    private static boolean containsExplicitWording(String text) {
        return text != null && EXPLICIT_WORDS.matcher(text).find();
    }
}
