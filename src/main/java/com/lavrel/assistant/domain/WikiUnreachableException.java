package com.lavrel.assistant.domain;

/** The wiki did not answer as a MediaWiki when an admin tried to approve it. Answered as 502. */
public class WikiUnreachableException extends RuntimeException {

    public WikiUnreachableException(String apiUrl) {
        super("The wiki at " + apiUrl + " did not answer as a MediaWiki (wrong address, moved, or down)");
    }
}
