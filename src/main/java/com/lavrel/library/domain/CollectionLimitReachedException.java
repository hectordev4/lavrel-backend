package com.lavrel.library.domain;

public class CollectionLimitReachedException extends RuntimeException {

    public CollectionLimitReachedException(int max) {
        super("You can have at most " + max + " collections");
    }
}
