package com.magenc.platform.iam.domain;

/**
 * Thrown when authentication fails for any reason.
 *
 * <p>The message is deliberately generic ("Invalid credentials") to avoid
 * leaking information about which step failed (user not found vs password
 * mismatch vs suspended account). This prevents user enumeration attacks.
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Invalid credentials");
    }
}
