package com.lavrel.user.domain;

/** A role change that is not allowed, such as an admin changing their own role (they could lock everyone out). */
public class InvalidRoleChangeException extends RuntimeException {

    public InvalidRoleChangeException(String message) {
        super(message);
    }
}
