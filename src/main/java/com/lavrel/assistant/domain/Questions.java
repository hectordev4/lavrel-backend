package com.lavrel.assistant.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Cleans what players type before it reaches the model, and the answer language they ask for. */
public final class Questions {

    public static final int MAX_LENGTH = 500;
    public static final String DEFAULT_LANGUAGE = "en";

    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\n]]");
    private static final Pattern SPACES = Pattern.compile("[ \\t]+");
    private static final Pattern LANGUAGE = Pattern.compile("[a-z]{2}");

    private Questions() {
    }

    /** @throws InvalidQuestionException if empty or longer than {@value #MAX_LENGTH} characters */
    public static String clean(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidQuestionException("Ask something about this achievement");
        }
        String text = SPACES.matcher(CONTROL.matcher(raw).replaceAll(" ")).replaceAll(" ").trim();
        if (text.codePointCount(0, text.length()) > MAX_LENGTH) {
            throw new InvalidQuestionException("A question has at most " + MAX_LENGTH + " characters");
        }
        return text;
    }

    /** Two-letter ISO code ("en", "es"...); anything else falls back to English. */
    public static String language(String raw) {
        if (raw == null) {
            return DEFAULT_LANGUAGE;
        }
        String code = raw.trim().toLowerCase(Locale.ROOT);
        return LANGUAGE.matcher(code).matches() ? code : DEFAULT_LANGUAGE;
    }
}
