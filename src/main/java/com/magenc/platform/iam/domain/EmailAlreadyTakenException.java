package com.magenc.platform.iam.domain;

public class EmailAlreadyTakenException extends RuntimeException {

    public EmailAlreadyTakenException(Email email) {
        super("Email is already registered: " + email);
    }
}
