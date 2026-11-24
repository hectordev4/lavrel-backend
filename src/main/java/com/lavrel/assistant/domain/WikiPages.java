package com.lavrel.assistant.domain;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One wiki often covers a whole franchise (the Dark Souls Wiki has Dark Souls I, II and III), and pages for the same
 * thing in different games are told apart by the game in parentheses: "Firelink Shrine (Dark Souls III)". Of the
 * search results, the pages about this game come first, then pages about no game in particular, then pages about
 * another game.
 */
public final class WikiPages {

    private static final Pattern PARENTHESES = Pattern.compile("\\(([^()]*)\\)\\s*$");
    private static final Pattern NOT_LETTER_OR_DIGIT = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    /** Trademark signs: NFKD would turn "™" into "TM" ("DARK SOULS™ III" must read as "dark souls iii"). */
    private static final Pattern TRADEMARKS = Pattern.compile("[\u2122\u00ae\u00a9\u2120]");

    private WikiPages() {
    }

    /** The best {@code limit} titles for this game, keeping the wiki's own order within each group. */
    public static List<String> preferFor(String gameTitle, List<String> titles, int limit) {
        String game = normalized(gameTitle);
        List<String> ranked = new ArrayList<>(titles);
        ranked.sort(Comparator.comparingInt(title -> rank(game, title)));
        return ranked.subList(0, Math.min(limit, ranked.size()));
    }

    /** 0: about this game; 1: about no game in particular; 2: about another game. */
    static int rank(String normalizedGame, String title) {
        Matcher m = PARENTHESES.matcher(title == null ? "" : title);
        if (!m.find()) {
            return 1;
        }
        String inside = normalized(m.group(1));
        if (inside.isEmpty() || normalizedGame.isEmpty()) {
            return 1;
        }
        return inside.equals(normalizedGame) || normalizedGame.endsWith(" " + inside) || inside.endsWith(normalizedGame)
                ? 0 : 2;
    }

    /** "DARK SOULS™ III" and "Dark Souls III" read the same; "Monster Hunter: World" as "monster hunter world". */
    static String normalized(String text) {
        if (text == null) {
            return "";
        }
        String noTrademarks = TRADEMARKS.matcher(text).replaceAll("");
        String plain = MARKS.matcher(Normalizer.normalize(noTrademarks, Normalizer.Form.NFKD)).replaceAll("");
        return NOT_LETTER_OR_DIGIT.matcher(plain.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }
}
