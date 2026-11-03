package com.lavrel.user.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The public name of a user: a free display name, like on Steam. It is NOT unique (the public id tells
 * people apart) and never comes from the Google account, so nobody's real name or e-mail is shown to others.
 * Everybody starts with a random friendly name (FriendlyNames) and can change it to whatever they like within
 * these rules:
 * 2 to 32 characters; letters of any alphabet (accents included), digits, spaces and a few symbols;
 * no names that pose as staff ("admin", "Support", "a.d.m.i.n"...).
 */
public final class Usernames {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 32;

    private static final Pattern ALLOWED = Pattern.compile("[\\p{L}\\p{M}\\p{N} _.\\-'!?&#()\\[\\]]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern NOT_LETTER_OR_DIGIT = Pattern.compile("[^\\p{L}\\p{N}]");
    private static final Set<String> RESERVED = Set.of("admin", "administrator", "moderator", "mod", "support",
            "staff", "system", "root", "lavrel", "lavrelteam", "lavrelsupport", "steam", "null", "undefined",
            "anonymous");

    private Usernames() {
    }

    /**
     * @return the name as it will be stored: trimmed, inner whitespace collapsed to single spaces,
     *         Unicode-normalized (NFC), with the capitalisation the user typed
     * @throws InvalidUsernameException if it is missing, too short or long, has characters outside the rules,
     *                                  or poses as staff
     */
    public static String validate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidUsernameException("A username is required");
        }
        String name = SPACES.matcher(Normalizer.normalize(raw, Normalizer.Form.NFC).trim()).replaceAll(" ");
        int length = name.codePointCount(0, name.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new InvalidUsernameException(
                    "The username must have " + MIN_LENGTH + " to " + MAX_LENGTH + " characters");
        }
        if (!ALLOWED.matcher(name).matches()) {
            throw new InvalidUsernameException(
                    "The username can use letters, numbers, spaces and these symbols: _ . - ' ! ? & # ( ) [ ]");
        }
        String bare = NOT_LETTER_OR_DIGIT.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("");
        if (RESERVED.contains(bare)) {
            throw new InvalidUsernameException("That username is reserved");
        }
        return name;
    }
}
