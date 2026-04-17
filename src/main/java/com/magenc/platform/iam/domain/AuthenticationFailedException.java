// Exists because auth failures must NEVER leak which step failed: same message for wrong user, wrong password, suspended account.
package com.magenc.platform.iam.domain;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() { super("Invalid credentials"); }
}
