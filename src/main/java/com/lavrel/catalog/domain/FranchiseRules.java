package com.lavrel.catalog.domain;

import com.lavrel.catalog.domain.GameMetadata.Creator;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Steam's "franchise" field is filled by publishers and is often not a franchise at all: the studio's own name
 * ("Team17 Digital" for Team17), a storefront or subscription brand ("EA Play"), or a placeholder ("FRANCHISE"). These
 * rules tell real franchises apart, at import and when an admin cleans the stored data. Pure functions.
 */
public final class FranchiseRules {

    /** Words that are not a franchise on their own. */
    static final Set<String> GENERIC = Set.of("franchise", "franchises", "series", "collection", "bundle", "saga");

    /** Company-name endings, ignored when comparing a franchise with the game's developers and publishers. */
    static final Set<String> CORPORATE = Set.of("digital", "games", "game", "studio", "studios", "entertainment",
            "interactive", "publishing", "publisher", "software", "inc", "ltd", "llc", "co", "corp", "corporation",
            "company", "sa", "gmbh", "srl", "kk", "limited", "group", "media", "productions", "the");

    private FranchiseRules() {
    }

    /** Lower case, no accents, letters and digits only: the key franchises are blocked and compared by. */
    public static String key(String name) {
        return String.join("", words(name));
    }

    /**
     * Why this is not a real franchise, or empty if it looks like one.
     * @param blockedKeys keys (see {@link #key}) an admin marked as not a franchise
     */
    public static Optional<String> rejectionReason(String franchise, Collection<String> developers,
                                                   Collection<String> publishers, Set<String> blockedKeys) {
        String key = key(franchise);
        if (key.isEmpty()) {
            return Optional.of("empty name");
        }
        if (blockedKeys.contains(key)) {
            return Optional.of("marked as not a franchise by an admin");
        }
        if (GENERIC.contains(key)) {
            return Optional.of("placeholder name");
        }
        String core = core(franchise);
        if (core.isEmpty()) {
            return Optional.of("only company words");
        }
        for (String developer : developers) {
            if (core.equals(core(developer))) {
                return Optional.of("same as the developer " + developer);
            }
        }
        for (String publisher : publishers) {
            if (core.equals(core(publisher))) {
                return Optional.of("same as the publisher " + publisher);
            }
        }
        return Optional.empty();
    }

    /** The franchises worth keeping for a game. */
    public static List<Creator> keepReal(List<Creator> franchises, List<Creator> developers, List<Creator> publishers,
                                         Set<String> blockedKeys) {
        List<String> developerNames = developers.stream().map(Creator::name).toList();
        List<String> publisherNames = publishers.stream().map(Creator::name).toList();
        return franchises.stream()
                .filter(f -> rejectionReason(f.name(), developerNames, publisherNames, blockedKeys).isEmpty())
                .toList();
    }

    /** The name without company endings: "CAPCOM Co., Ltd." and "Capcom" both give "capcom". */
    static String core(String name) {
        return words(name).stream().filter(w -> !CORPORATE.contains(w)).collect(Collectors.joining());
    }

    private static List<String> words(String name) {
        if (name == null) {
            return List.of();
        }
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(plain.split("[^a-z0-9]+")).filter(w -> !w.isEmpty()).toList();
    }
}
