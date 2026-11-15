package com.lavrel.user.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The public name of a user: a free display name, like on Steam. It is NOT unique (the public id tells
 * people apart) and never comes from the Google account, so nobody's real name or e-mail is shown to others.
 * Everybody starts with a random friendly name (FriendlyNames) and can change it to whatever they like within
 * these rules:
 * 2 to 32 characters; letters of any alphabet (accents included), digits, spaces and a few symbols;
 * no names that pose as Lavrel or its staff (see isReserved). Staff (SUPPORT, ADMIN) may use those.
 */
public final class Usernames {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 32;

    private static final Pattern ALLOWED = Pattern.compile("[\\p{L}\\p{M}\\p{N} _.\\-'!?&#()\\[\\]]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern NOT_LETTER = Pattern.compile("[^\\p{L}]");
    private static final Pattern WORD_BREAK = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    /** The brand: blocked anywhere in the name ("Lavrel Support", "TheLavrelTeam", "L4vrel"). */
    private static final String BRAND = "lavrel";

    /**
     * Blocked as the whole name, ignoring spaces, symbols and digits ("Support", "a.d.m.i.n", "Admin 2"). Words that
     * are also common gamer words ("support", "mod", "dev") are only blocked on their own, so "Support Main" is fine.
     */
    private static final Set<String> RESERVED_NAMES = Set.of("admin", "admins", "administrator", "administrador",
            "moderator", "moderador", "mod", "mods", "support", "soporte", "helpdesk", "help", "staff",
            "official", "oficial", "system", "sistema", "root", "dev", "devs", "developer", "owner", "steam",
            "xbox", "playstation", "psn", "null", "undefined", "anonymous", "deleted", "deleteduser");

    /** Blocked as any word of the name ("Head Admin", "Official Steve", "Moderator Kai"). */
    private static final Set<String> STAFF_WORDS = Set.of("admin", "admins", "administrator", "administrador",
            "moderator", "moderador", "staff", "official", "oficial");

    /** Digits and look-alike letters used to dodge the list: "4dm1n", "аdmin" with a Cyrillic а. */
    private static final Map<Character, Character> LOOK_ALIKES = Map.ofEntries(
            Map.entry('0', 'o'), Map.entry('1', 'i'), Map.entry('3', 'e'), Map.entry('4', 'a'), Map.entry('5', 's'),
            Map.entry('7', 't'), Map.entry('а', 'a'), Map.entry('е', 'e'), Map.entry('о', 'o'), Map.entry('р', 'p'),
            Map.entry('с', 'c'), Map.entry('х', 'x'), Map.entry('і', 'i'), Map.entry('ӏ', 'l'), Map.entry('ѕ', 's'),
            Map.entry('ԁ', 'd'), Map.entry('ı', 'i'));

    private Usernames() {
    }

    /**
     * @return the name as it will be stored: trimmed, inner whitespace collapsed to single spaces,
     *         Unicode-normalized (NFC), with the capitalisation the user typed
     * @throws InvalidUsernameException  if it is missing, too short or long, or has characters outside the rules
     * @throws UsernameReservedException if it poses as Lavrel or its staff
     */
    public static String validate(String raw) {
        return validate(raw, false);
    }

    /**
     * Same as validate(raw), but staff (SUPPORT, ADMIN) may use the reserved names: they really are Lavrel staff.
     */
    public static String validate(String raw, boolean staff) {
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
        if (!staff && isReserved(name)) {
            throw new UsernameReservedException();
        }
        return name;
    }

    /**
     * True if the name poses as Lavrel or its staff. Compared without capitals, accents, spaces, symbols or digits,
     * read twice: as typed ("Admin123" is "admin") and with look-alike digits and letters read as the letter they
     * imitate ("4dm1n", "аdmin" with a Cyrillic а). Reserved: the brand anywhere; a reserved name as the whole name;
     * a staff word as any word.
     */
    static boolean isReserved(String name) {
        String plain = plain(name);
        return isReservedReading(plain) || isReservedReading(lookAlikesAsLetters(plain));
    }

    private static boolean isReservedReading(String reading) {
        String letters = NOT_LETTER.matcher(reading).replaceAll("");
        if (letters.contains(BRAND) || RESERVED_NAMES.contains(letters)) {
            return true;
        }
        return Arrays.stream(WORD_BREAK.split(reading))
                .map(word -> NOT_LETTER.matcher(word).replaceAll(""))
                .anyMatch(STAFF_WORDS::contains);
    }

    /** Lower case, accents removed. */
    private static String plain(String name) {
        return MARKS.matcher(Normalizer.normalize(name, Normalizer.Form.NFKD)).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /** Look-alike digits and letters replaced by the letter they imitate. */
    private static String lookAlikesAsLetters(String plain) {
        StringBuilder out = new StringBuilder(plain.length());
        for (char c : plain.toCharArray()) {
            out.append(LOOK_ALIKES.getOrDefault(c, c));
        }
        return out.toString();
    }
}
