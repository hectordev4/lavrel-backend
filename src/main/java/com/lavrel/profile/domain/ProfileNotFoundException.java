package com.lavrel.profile.domain;

/** No public profile with that public id: the user does not exist or keeps the profile private (not told apart). */
public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException() {
        super("No public profile with that id");
    }
}
