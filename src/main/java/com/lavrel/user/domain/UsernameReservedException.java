package com.lavrel.user.domain;

/** The name poses as Lavrel or its staff ("Lavrel Support", "Admin", "4dm1n"...). Answered as username_reserved. */
public class UsernameReservedException extends InvalidUsernameException {
    public UsernameReservedException() {
        super("That name is reserved for Lavrel staff");
    }
}
