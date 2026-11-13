package com.lavrel.user.domain;

/**
 * The platform account is already linked to another Lavrel user. The message never says which user: it reaches the
 * person trying to link.
 */
public class AccountLinkedElsewhereException extends RuntimeException {

    public AccountLinkedElsewhereException() {
        super("This account is already linked to another Lavrel user");
    }
}
