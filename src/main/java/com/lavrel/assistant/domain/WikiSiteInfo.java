package com.lavrel.assistant.domain;

/**
 * What a wiki says about itself (MediaWiki siteinfo): its name, where it is served from, its content license and how
 * many articles it has. Read when an admin approves the wiki, never trusted from the admin's own typing.
 *
 * @param server     "https://darksouls.fandom.com" (may come without the scheme: "//darksouls.fandom.com")
 * @param articleUrl where its pages live, with $1 for the title (server + articlepath), or null
 */
public record WikiSiteInfo(String siteName, String server, String licenseUrl, String licenseText, String articleUrl,
                           long articles) {

    /** The host the wiki says it is served from, lower case, or "" if it did not say. */
    public String serverHost() {
        if (server == null) {
            return "";
        }
        String s = server.replaceFirst("^(https?:)?//", "");
        int slash = s.indexOf('/');
        return (slash < 0 ? s : s.substring(0, slash)).toLowerCase(java.util.Locale.ROOT);
    }
}
