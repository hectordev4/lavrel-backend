package com.lavrel.assistant.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns MediaWiki markup into plain text and cuts out the part about an achievement, so the model gets a short,
 * relevant excerpt instead of a whole page. Pure functions.
 */
public final class WikiText {

    private static final Pattern COMMENTS = Pattern.compile("(?s)<!--.*?-->");
    private static final Pattern REFS = Pattern.compile("(?is)<ref[^>]*/>|<ref[^>]*>.*?</ref>");
    private static final Pattern INNER_TEMPLATE = Pattern.compile("\\{\\{[^{}]*}}");
    private static final Pattern FILE_LINKS = Pattern.compile("(?i)\\[\\[(file|image|category):[^\\[\\]]*]]");
    private static final Pattern PIPED_LINK = Pattern.compile("\\[\\[[^\\[\\]|]*\\|([^\\[\\]]*)]]");
    private static final Pattern LINK = Pattern.compile("\\[\\[([^\\[\\]]*)]]");
    private static final Pattern EXTERNAL_LINK = Pattern.compile("\\[https?://\\S+\\s+([^\\]]*)]");
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");
    private static final Pattern HEADINGS = Pattern.compile("(?m)^=+\\s*(.*?)\\s*=+\\s*$");
    private static final Pattern TABLE_LINES = Pattern.compile("(?m)^\\s*(\\{\\||\\|}|\\|-).*$");
    private static final Pattern TABLE_CELLS = Pattern.compile("(?m)^\\s*[|!]\\s*");
    private static final Pattern SPACES = Pattern.compile("[ \\t]+");
    private static final Pattern BLANK_LINES = Pattern.compile("\\n{3,}");

    private WikiText() {
    }

    public static String clean(String wikitext) {
        if (wikitext == null) {
            return "";
        }
        String s = COMMENTS.matcher(wikitext).replaceAll("");
        s = REFS.matcher(s).replaceAll("");
        for (int i = 0; i < 30; i++) {
            String next = INNER_TEMPLATE.matcher(s).replaceAll("");
            if (next.equals(s)) {
                break;
            }
            s = next;
        }
        s = FILE_LINKS.matcher(s).replaceAll("");
        s = PIPED_LINK.matcher(s).replaceAll("$1");
        s = LINK.matcher(s).replaceAll("$1");
        s = EXTERNAL_LINK.matcher(s).replaceAll("$1");
        s = s.replace("'''", "").replace("''", "");
        s = HEADINGS.matcher(s).replaceAll("$1:");
        s = TABLE_LINES.matcher(s).replaceAll("");
        s = TABLE_CELLS.matcher(s).replaceAll("");
        s = s.replace("||", " | ").replace("!!", " | ");
        s = TAGS.matcher(s).replaceAll("");
        s = SPACES.matcher(s).replaceAll(" ");
        return BLANK_LINES.matcher(s).replaceAll("\n\n").trim();
    }

    /**
     * Up to maxChars of text around the first mention of the term (a little before it, most after); the start of the
     * text when the term is not mentioned.
     */
    public static String around(String text, String term, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        int at = term == null ? -1 : text.toLowerCase(Locale.ROOT).indexOf(term.toLowerCase(Locale.ROOT));
        int start = at < 0 ? 0 : Math.max(0, at - maxChars / 5);
        int end = Math.min(text.length(), start + maxChars);
        return (start > 0 ? "…" : "") + text.substring(start, end).trim() + (end < text.length() ? "…" : "");
    }
}
