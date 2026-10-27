package com.lavrel.catalog.domain;

import com.lavrel.catalog.config.CatalogFilterProperties;
import com.lavrel.catalog.domain.GameMetadata.TagWeight;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Content filter. Returns the rejection reason, or empty when the game passes.
 *
 * Category : appdetails "type" must be "game" (no DLC, music, video, demo).
 * Layer 1  : descriptor 3 (Adult Only Sexual Content) rejects outright.
 *            Descriptor 4 (Frequent Nudity or Sexual Content) rejects only when a sexual tag
 *            ranks in the top strictDescriptorMaxRank, at any share.
 * Layer 2  : sexual tag share of the top-N weight sum; reject when share >= rejectShare AND
 *            rank <= rejectMaxRank. Everything else (rank 4+, or a smaller share) passes.
 * Layer 3  : excluded tags (any rank) and excluded store genres.
 */
public final class ContentPolicy {

    private final CatalogFilterProperties rules;
    private final Set<String> excludedGenres;

    public ContentPolicy(CatalogFilterProperties rules) {
        this.rules = rules;
        this.excludedGenres = rules.excludedGenres().stream()
                .map(g -> g.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public Optional<String> evaluate(GameSignals game) {
        if (!rules.enabled()) {
            return Optional.empty();
        }
        List<TagWeight> ranked = rank(game.tags());
        return checkCategory(game)
                .or(() -> checkDescriptors(game, ranked))
                .or(() -> checkTagShare(ranked))
                .or(() -> checkExclusions(game));
    }

    private static List<TagWeight> rank(List<TagWeight> tags) {
        return nullSafe(tags).stream()
                .sorted(Comparator.comparingInt(TagWeight::weight).reversed())
                .toList();
    }

    private Optional<String> checkCategory(GameSignals game) {
        String type = game.type();
        if (type != null && !rules.requiredType().equalsIgnoreCase(type.trim())) {
            return Optional.of("not a game: type " + type.trim());
        }
        return Optional.empty();
    }

    // Layer 1
    private Optional<String> checkDescriptors(GameSignals game, List<TagWeight> ranked) {
        List<Integer> descriptors = nullSafe(game.contentDescriptorIds());
        for (Integer id : descriptors) {
            if (id != null && rules.rejectedDescriptorIds().contains(id)) {
                return Optional.of("adult content: descriptor " + id + " (Adult Only Sexual Content)");
            }
        }
        Integer strictId = descriptors.stream()
                .filter(id -> id != null && rules.strictDescriptorIds().contains(id))
                .findFirst()
                .orElse(null);
        if (strictId == null) {
            return Optional.empty();
        }
        int limit = Math.min(rules.strictDescriptorMaxRank(), ranked.size());
        for (int i = 0; i < limit; i++) {
            long tagId = ranked.get(i).tagId();
            if (rules.sexualTagIds().contains(tagId)) {
                return Optional.of("adult content: descriptor " + strictId + " with tag " + tagId + " at rank " + (i + 1));
            }
        }
        return Optional.empty();
    }

    // Layer 2
    private Optional<String> checkTagShare(List<TagWeight> ranked) {
        List<TagWeight> top = ranked.subList(0, Math.min(rules.normalizationCount(), ranked.size()));
        long total = top.stream().mapToLong(TagWeight::weight).sum();
        if (total <= 0) {
            return Optional.empty();
        }
        for (int i = 0; i < top.size(); i++) {
            TagWeight tag = top.get(i);
            if (!rules.sexualTagIds().contains(tag.tagId())) {
                continue;
            }
            int rank = i + 1;
            double share = (double) tag.weight() / total;
            if (share >= rules.rejectShare() && rank <= rules.rejectMaxRank()) {
                return Optional.of(String.format(Locale.ROOT,
                        "adult content: tag %d at rank %d with %.1f%% of the top-%d votes",
                        tag.tagId(), rank, share * 100, top.size()));
            }
        }
        return Optional.empty();
    }

    // Layer 3
    private Optional<String> checkExclusions(GameSignals game) {
        for (TagWeight tag : nullSafe(game.tags())) {
            if (rules.excludedTagIds().contains(tag.tagId())) {
                return Optional.of("excluded tag " + tag.tagId());
            }
        }
        for (String genre : nullSafe(game.genres())) {
            if (genre != null && excludedGenres.contains(genre.trim().toLowerCase(Locale.ROOT))) {
                return Optional.of("excluded genre " + genre.trim());
            }
        }
        return Optional.empty();
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
