package com.lavrel.user.domain;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The public name of a user. It never comes from the Google account: nobody's real name or e-mail is
 * shown to others. Everybody starts with a random name and can change it to one they choose.
 */
public final class Usernames {

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_]{3,20}");
    private static final Set<String> RESERVED = Set.of("admin", "administrator", "moderator", "mod", "support",
            "staff", "system", "root", "lavrel", "steam", "null", "undefined", "anonymous");
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Usernames() {
    }

    /** A name like "player_k7x2qa9d". Uniqueness is checked by the caller. */
    public static String random() {
        StringBuilder name = new StringBuilder("player_");
        for (int i = 0; i < 8; i++) {
            name.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return name.toString();
    }

    /**
     * @return the name, trimmed, with the capitalisation the user typed
     * @throws InvalidUsernameException if it is missing, not 3-20 letters, digits or underscores, or reserved
     */
    public static String validate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidUsernameException("A username is required");
        }
        String name = raw.trim();
        if (!VALID.matcher(name).matches()) {
            throw new InvalidUsernameException("The username must have 3 to 20 characters: letters, numbers or underscore");
        }
        if (RESERVED.contains(name.toLowerCase(Locale.ROOT))) {
            throw new InvalidUsernameException("That username is reserved");
        }
        return name;
    }
}
