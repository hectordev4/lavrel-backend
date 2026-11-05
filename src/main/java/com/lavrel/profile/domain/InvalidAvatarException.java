package com.lavrel.profile.domain;

/** The file is not an accepted picture (type, size, dimensions or frames). */
public class InvalidAvatarException extends RuntimeException {

    public InvalidAvatarException(String message) {
        super(message);
    }
}
