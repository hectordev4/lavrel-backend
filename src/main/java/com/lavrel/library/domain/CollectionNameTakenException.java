package com.lavrel.library.domain;

public class CollectionNameTakenException extends RuntimeException {

    public CollectionNameTakenException(String name) {
        super("You already have a collection called " + name);
    }
}
