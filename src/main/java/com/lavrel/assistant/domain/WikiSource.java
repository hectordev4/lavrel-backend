package com.lavrel.assistant.domain;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A wiki an admin approved as reference for one game: its MediaWiki API address (Fandom, wiki.gg and most game wikis
 * run MediaWiki) and its content license, which the admin checked allows reuse (e.g. CC BY-SA 3.0). Answers built from
 * it show its pages and license.
 */
public record WikiSource(long gameId, String apiUrl, String license, Instant addedAt) {

    private static final Pattern HOST = Pattern.compile("[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+");

    public WikiSource {
        apiUrl = validApiUrl(apiUrl);
        license = validLicense(license);
        Objects.requireNonNull(addedAt, "addedAt");
    }

    /**
     * https only, a real domain name (no IP addresses, no localhost: the backend must never be pointed at internal
     * machines), and the MediaWiki API path, e.g. https://hades.fandom.com/api.php.
     */
    static String validApiUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidWikiSourceException("apiUrl is required, e.g. https://hades.fandom.com/api.php");
        }
        URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidWikiSourceException("apiUrl is not a valid address");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equals(uri.getScheme()) || uri.getUserInfo() != null || uri.getPort() != -1
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new InvalidWikiSourceException("apiUrl must be a plain https address");
        }
        if (!HOST.matcher(host).matches() || host.equals("localhost") || host.endsWith(".local")
                || host.chars().allMatch(c -> Character.isDigit(c) || c == '.')) {
            throw new InvalidWikiSourceException("apiUrl must use a public domain name");
        }
        if (uri.getPath() == null || !uri.getPath().endsWith("/api.php")) {
            throw new InvalidWikiSourceException("apiUrl must point at the wiki's api.php");
        }
        return "https://" + host + uri.getPath();
    }

    static String validLicense(String raw) {
        if (raw == null || raw.isBlank() || raw.trim().length() > 100) {
            throw new InvalidWikiSourceException("license is required (e.g. CC BY-SA 3.0), at most 100 characters");
        }
        return raw.trim();
    }

    /** The address of a page of this wiki, for the answer's sources. */
    public String pageUrl(String title) {
        String base = apiUrl.substring(0, apiUrl.length() - "api.php".length());
        return base + "wiki/" + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8)
                .replace("%2F", "/").replace("%3A", ":");
    }
}
