package com.lavrel.assistant.domain;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Search terms for a game's English wiki. A chat question is often in another language ("¿Dónde consigo el anillo de
 * la serpiente de oro?"), so the model first turns it into a few English words ("Covetous Gold Serpent Ring
 * location"). What the model writes is checked here; anything unusable falls back to the player's own question.
 */
public final class SearchTerms {

    static final int MAX_LENGTH = 80;

    private static final Pattern LABEL = Pattern.compile("(?i)^(search terms?|terms?|keywords?)\\s*:\\s*");
    private static final Pattern QUOTES = Pattern.compile("[\"'`“”‘’«»*]");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern LETTER = Pattern.compile("\\p{L}");
    private static final Pattern WORDS = Pattern.compile("[^\\p{L}\\p{N}]+");

    /** Words that say what the player wants, not what to look for: never the word the excerpt is cut around. */
    private static final Set<String> GENERIC = Set.of("location", "locations", "where", "find", "obtain", "get",
            "how", "guide", "walkthrough", "farm", "farming", "beat", "defeat", "kill", "best", "build", "builds",
            "all", "list", "unlock", "achievement", "trophy", "strategy", "tips", "boss", "bosses");

    private SearchTerms() {
    }

    /**
     * The first usable line of the model's answer, without quotes or a "Search terms:" label, at most 80 characters.
     *
     * @return the cleaned terms, or the fallback (the player's question) if nothing usable is left
     */
    public static String clean(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        for (String line : raw.split("\\R")) {
            String terms = SPACES.matcher(QUOTES.matcher(LABEL.matcher(line.trim()).replaceFirst("")).replaceAll(" "))
                    .replaceAll(" ").trim();
            if (terms.endsWith(".")) {
                terms = terms.substring(0, terms.length() - 1).trim();
            }
            if (!terms.isEmpty() && LETTER.matcher(terms).find()) {
                return terms.length() <= MAX_LENGTH ? terms : terms.substring(0, MAX_LENGTH).trim();
            }
        }
        return fallback;
    }

    /**
     * The most telling word, to cut a wiki page around it: the longest one of at least 4 characters that is not a
     * generic word ("Covetous" in "Covetous Gold Serpent Ring location"). Null if there is none.
     */
    public static String focusWord(String terms) {
        String best = null;
        for (String word : WORDS.split(terms == null ? "" : terms)) {
            if (word.length() >= 4 && !GENERIC.contains(word.toLowerCase(Locale.ROOT))
                    && (best == null || word.length() > best.length())) {
                best = word;
            }
        }
        return best;
    }
}
