package com.lavrel.user.domain;

/**
 * Linking an account could not be confirmed. The reason tells the frontend what to say:
 *  - LINK_EXPIRED: the link request is older than its lifetime (10 minutes); start again;
 *  - INVALID_LINK_STATE: the link request is missing, damaged or was started for another platform;
 *  - NOT_CONFIRMED: the platform (Steam OpenID) did not confirm that the user owns the account.
 */
public class InvalidIdentityProofException extends RuntimeException {

    public enum Reason {
        LINK_EXPIRED, INVALID_LINK_STATE, NOT_CONFIRMED
    }

    private final Reason reason;

    public InvalidIdentityProofException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    /** The platform did not confirm the identity. */
    public InvalidIdentityProofException(String message) {
        this(Reason.NOT_CONFIRMED, message);
    }

    public Reason reason() {
        return reason;
    }
}
