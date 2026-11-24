package com.lavrel.assistant.domain;

/**
 * A piece of a reference page given to the model, with where it came from: the page, the wiki it belongs to and the
 * wiki's license (CC BY-SA asks for this credit, shown under the answer).
 *
 * @param site       the wiki's own name ("Dark Souls Wiki"), or null if unknown
 * @param licenseUrl the license's address, or null if unknown
 */
public record Excerpt(String title, String url, String site, String license, String licenseUrl, String text) {

    /** Without the wiki's name or license link. */
    public Excerpt(String title, String url, String license, String text) {
        this(title, url, null, license, null, text);
    }

    public Guide.Source source() {
        return new Guide.Source(title, url, site, license, licenseUrl);
    }
}
