package com.lavrel.assistant.domain;

public class InvalidQuestionException extends RuntimeException {

    public InvalidQuestionException(String message) {
        super(message);
    }
}
