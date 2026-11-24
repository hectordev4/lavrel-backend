package com.lavrel.assistant.domain;

/** The wiki's own license does not allow reuse (only CC BY-SA, not non-commercial). Answered as 400. */
public class WikiLicenseNotAllowedException extends RuntimeException {

    public WikiLicenseNotAllowedException(String message) {
        super(message);
    }
}
