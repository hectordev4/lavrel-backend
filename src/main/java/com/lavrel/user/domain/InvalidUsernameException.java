package com.lavrel.user.domain;

public class InvalidUsernameException extends RuntimeException {
    public InvalidUsernameException(String message) {
        super(message);
    }
}
