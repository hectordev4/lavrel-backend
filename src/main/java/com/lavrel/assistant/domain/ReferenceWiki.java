package com.lavrel.assistant.domain;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A wiki the assistant may read, shared by every game linked to it (one Fandom wiki often covers a whole franchise:
 * Monster Hunter World, Rise and Wilds use the same one). Fandom, wiki.gg and most game wikis run MediaWiki, so it is
 * reached through its API address. Its name, license and license link are the ones the wiki itself declares, read
 * when an admin approves it, and are shown as the credit under every answer built from it.
 *
 * @param id         null until stored
 * @param siteName   the wiki's own name ("Dark Souls Wiki"); null for wikis approved before V35, until approved again
 * @param articleUrl where its pages live, with $1 for the title ("https://bg3.wiki/wiki/$1"); null means the
 *                   address next to api.php ("https://hades.fandom.com/wiki/$1")
 */
public record ReferenceWiki(Long id, String apiUrl, String siteName, String license, String licenseUrl,
                            String articleUrl, Instant addedAt) {

    private static final Pattern HOST = Pattern.compile("[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+");

    public ReferenceWiki {
        apiUrl = validApiUrl(apiUrl);
        license = validLicense(license);
        siteName = clip(siteName, 200);
        licenseUrl = clip(licenseUrl, 300);
        articleUrl = articleUrl == null || !articleUrl.startsWith("https://") || !articleUrl.contains("$1")
                ? null : clip(articleUrl, 300);
        Objects.requireNonNull(addedAt, "addedAt");
    }

    /** A wiki just checked by an admin, from what the wiki says about itself. */
    public static ReferenceWiki approved(String apiUrl, WikiSiteInfo info, String license, Instant now) {
        return new ReferenceWiki(null, apiUrl, info.siteName(), license, info.licenseUrl(), info.articleUrl(), now);
    }

    /** The same wiki, with what it says about itself today (name and license can change). */
    public ReferenceWiki refreshedFrom(WikiSiteInfo info, String license) {
        return new ReferenceWiki(id, apiUrl, info.siteName(), license, info.licenseUrl(), info.articleUrl(), addedAt);
    }

    public ReferenceWiki withId(long newId) {
        return new ReferenceWiki(newId, apiUrl, siteName, license, licenseUrl, articleUrl, addedAt);
    }

    /**
     * https only, a real domain name (no IP addresses, no localhost: the backend must never be pointed at internal
     * machines), and the MediaWiki API path, e.g. https://hades.fandom.com/api.php or https://bg3.wiki/w/api.php.
     */
    public static String validApiUrl(String raw) {
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

    public String host() {
        return URI.create(apiUrl).getHost();
    }

    /** The address of a page of this wiki, for the answer's sources. */
    public String pageUrl(String title) {
        String encoded = URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8)
                .replace("%2F", "/").replace("%3A", ":");
        if (articleUrl != null) {
            return articleUrl.replace("$1", encoded);
        }
        String base = apiUrl.substring(0, apiUrl.length() - "api.php".length());
        return base + "wiki/" + encoded;
    }

    static String validLicense(String raw) {
        if (raw == null || raw.isBlank() || raw.trim().length() > 100) {
            throw new InvalidWikiSourceException("license is required (e.g. CC BY-SA 3.0), at most 100 characters");
        }
        return raw.trim();
    }

    private static String clip(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
