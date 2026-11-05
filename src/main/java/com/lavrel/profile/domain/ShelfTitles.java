package com.lavrel.profile.domain;

import java.util.regex.Pattern;

/** Optional shelf titles: blank means none (the frontend names the shelf by its type); otherwise 1 to 60 characters. */
public final class ShelfTitles {

    public static final int MAX_LENGTH = 60;

    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");

    private ShelfTitles() {
    }

    /** @return the cleaned title, or null for none. @throws InvalidShelfException if too long or with control chars */
    public static String validate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String title = SPACES.matcher(raw.trim()).replaceAll(" ");
        if (CONTROL.matcher(title).find()) {
            throw new InvalidShelfException("The title has characters that are not allowed");
        }
        if (title.codePointCount(0, title.length()) > MAX_LENGTH) {
            throw new InvalidShelfException("A shelf title has at most " + MAX_LENGTH + " characters");
        }
        return title;
    }
}
