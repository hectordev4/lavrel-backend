package com.lavrel.assistant.application.port.out;

import java.util.List;
import java.util.Optional;

/** Reads a MediaWiki wiki through its public API. Failures return empty results, never errors. */
public interface WikiClientPort {

    /** Page titles matching the text, best first. */
    List<String> search(String apiUrl, String text, int limit);

    /** The page's markup, following redirects. */
    Optional<String> wikitext(String apiUrl, String title);
}
