package com.lavrel.library.domain;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Collection names: 1 to 40 characters, trimmed, inner spaces collapsed, no control characters. */
public final class CollectionNames {

    public static final int MAX_LENGTH = 40;

    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");

    private CollectionNames() {
    }

    /** @throws InvalidCollectionNameException if it is empty, too long or has control characters */
    public static String validate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidCollectionNameException("A collection needs a name");
        }
        String trimmed = Normalizer.normalize(raw, Normalizer.Form.NFC).trim();
        if (CONTROL.matcher(trimmed.replace('\t', ' ')).find()) {
            throw new InvalidCollectionNameException("The name has characters that are not allowed");
        }
        String name = SPACES.matcher(trimmed).replaceAll(" ");
        if (name.codePointCount(0, name.length()) > MAX_LENGTH) {
            throw new InvalidCollectionNameException("A collection name has at most " + MAX_LENGTH + " characters");
        }
        return name;
    }
}
