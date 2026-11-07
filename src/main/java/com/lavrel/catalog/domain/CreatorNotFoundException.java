package com.lavrel.catalog.domain;

public class CreatorNotFoundException extends RuntimeException {

    public CreatorNotFoundException(long creatorId) {
        super("Creator " + creatorId + " not found");
    }
}
