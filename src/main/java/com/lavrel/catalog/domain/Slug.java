package com.lavrel.catalog.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The readable part of a game's address: "The Witcher 3: Wild Hunt" becomes "the-witcher-3-wild-hunt",
 * so a shared link reads /games/292030-the-witcher-3-wild-hunt. Accents are removed. The slug is cosmetic:
 * the id in front of it is what identifies the game.
 */
public final class Slug {

    private static final int MAX_LENGTH = 60;
    private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern ACCENTS = Pattern.compile("\\p{M}+");
    private static final String FALLBACK = "game";

    private Slug() {
    }

    /** Never empty: titles with no Latin letters or digits (for example Japanese ones) give "game". */
    public static String of(String title) {
        if (title == null) {
            return FALLBACK;
        }
        String withoutAccents = ACCENTS.matcher(Normalizer.normalize(title, Normalizer.Form.NFD)).replaceAll("");
        String slug = NOT_ALPHANUMERIC.matcher(withoutAccents.toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = trimDashes(slug);
        if (slug.length() > MAX_LENGTH) {
            slug = trimDashes(slug.substring(0, MAX_LENGTH));
        }
        return slug.isEmpty() ? FALLBACK : slug;
    }

    private static String trimDashes(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '-') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '-') {
            end--;
        }
        return value.substring(start, end);
    }
}
