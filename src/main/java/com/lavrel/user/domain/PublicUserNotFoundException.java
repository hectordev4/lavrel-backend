package com.lavrel.user.domain;

/**
 * No user with that PUBLIC id. The id may appear in the message: it is public anyway, and the caller sent it.
 * Used by every lookup of another person (admin tools now; profiles, comments and player search later).
 */
public class PublicUserNotFoundException extends RuntimeException {

    private final long publicId;

    public PublicUserNotFoundException(long publicId) {
        super("No user with public id " + publicId);
        this.publicId = publicId;
    }

    public long publicId() {
        return publicId;
    }
}
