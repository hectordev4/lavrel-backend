package com.lavrel.user.domain;

/** The account may not use the forced verification (not on the allowlist). Answered as 404, as if it did not exist. */
public class EmailVerificationNotAllowedException extends RuntimeException {

    public EmailVerificationNotAllowedException() {
        super("Not found");
    }
}
