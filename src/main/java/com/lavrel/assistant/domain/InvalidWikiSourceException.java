package com.lavrel.assistant.domain;

public class InvalidWikiSourceException extends RuntimeException {

    public InvalidWikiSourceException(String message) {
        super(message);
    }
}
