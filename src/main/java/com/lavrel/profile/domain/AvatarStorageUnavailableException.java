package com.lavrel.profile.domain;

/** Picture storage is not configured or did not answer; the user's avatar is left as it was. */
public class AvatarStorageUnavailableException extends RuntimeException {

    public AvatarStorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
