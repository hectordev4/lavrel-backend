package com.lavrel.user.domain;

public class EmailAlreadyVerifiedException extends RuntimeException {

    public EmailAlreadyVerifiedException() {
        super("This e-mail is already verified");
    }
}
