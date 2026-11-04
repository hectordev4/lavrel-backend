package com.lavrel.user.domain;

/** The only admin cannot delete their account: promote someone else first, so nobody is locked out. */
public class LastAdminException extends RuntimeException {

    public LastAdminException() {
        super("You are the only admin. Make someone else an admin before deleting your account.");
    }
}
