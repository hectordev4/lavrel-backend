package com.lavrel.profile.domain;

public class ShelfLimitReachedException extends RuntimeException {

    public ShelfLimitReachedException(int max) {
        super("A profile has at most " + max + " shelves");
    }
}
