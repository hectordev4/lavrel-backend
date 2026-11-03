package com.lavrel.tracking.domain;

public class QuickAccessFullException extends RuntimeException {

    public QuickAccessFullException(int max) {
        super("Quick access holds at most " + max + " games; unpin one first");
    }
}
