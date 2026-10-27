package com.lavrel.catalog.domain;

/** A comparison that makes no sense, such as comparing Steam (the base list) with itself. */
public class InvalidComparisonException extends RuntimeException {

    public InvalidComparisonException(String message) {
        super(message);
    }
}
