package com.lavrel.user.domain;

public class UsernameTakenException extends RuntimeException {
    public UsernameTakenException(String username) {
        super("The username is already taken: " + username);
    }
}
