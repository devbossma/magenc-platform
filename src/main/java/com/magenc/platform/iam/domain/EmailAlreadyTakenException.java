// Exists because duplicate email at signup is a domain rule violation, not a database error.
package com.magenc.platform.iam.domain;

public class EmailAlreadyTakenException extends RuntimeException {
    public EmailAlreadyTakenException(Email email) { super("Email is already registered: " + email); }
}
