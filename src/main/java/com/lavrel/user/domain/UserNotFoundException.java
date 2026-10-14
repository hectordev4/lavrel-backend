package com.lavrel.user.domain;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(long userId) {
        super("User not found: " + userId);
    }
}
