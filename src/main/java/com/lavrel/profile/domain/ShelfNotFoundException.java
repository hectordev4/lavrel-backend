package com.lavrel.profile.domain;

/** No such shelf, or it belongs to somebody else (not told apart). */
public class ShelfNotFoundException extends RuntimeException {

    public ShelfNotFoundException() {
        super("Shelf not found");
    }
}
