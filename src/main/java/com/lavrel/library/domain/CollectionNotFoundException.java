package com.lavrel.library.domain;

/** No such collection, or it belongs to somebody else (the two are not told apart). */
public class CollectionNotFoundException extends RuntimeException {

    public CollectionNotFoundException() {
        super("Collection not found");
    }
}
