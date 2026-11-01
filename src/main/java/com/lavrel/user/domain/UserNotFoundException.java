package com.lavrel.user.domain;

/**
 * No user with that INTERNAL id (the database id). The message never contains the id, because it reaches the
 * client and the database id must not leave the backend; the id is kept for the server log.
 * For lookups by public id, use PublicUserNotFoundException.
 */
public class UserNotFoundException extends RuntimeException {

    private final long userId;

    public UserNotFoundException(long userId) {
        super("User not found");
        this.userId = userId;
    }

    public long userId() {
        return userId;
    }
}
