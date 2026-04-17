// Exists because password policy is enforced at construction time: if you hold a RawPassword, it satisfies all rules.
package com.magenc.platform.iam.domain;

import java.util.regex.Pattern;

public final class RawPassword {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9]");

    private final String value;

    private RawPassword(String value) { this.value = value; }

    public static RawPassword of(String value) {
        if (value == null) throw new WeakPasswordException("Password cannot be null");
        if (value.length() < MIN_LENGTH) throw new WeakPasswordException("Password must be at least " + MIN_LENGTH + " characters");
        if (value.length() > MAX_LENGTH) throw new WeakPasswordException("Password must be at most " + MAX_LENGTH + " characters");
        if (!UPPERCASE.matcher(value).find()) throw new WeakPasswordException("Password must contain at least one uppercase letter");
        if (!LOWERCASE.matcher(value).find()) throw new WeakPasswordException("Password must contain at least one lowercase letter");
        if (!DIGIT.matcher(value).find()) throw new WeakPasswordException("Password must contain at least one digit");
        if (!SYMBOL.matcher(value).find()) throw new WeakPasswordException("Password must contain at least one symbol");
        return new RawPassword(value);
    }

    public String value() { return value; }
    @Override public String toString() { return "RawPassword[REDACTED]"; }

    public static class WeakPasswordException extends RuntimeException {
        public WeakPasswordException(String message) { super(message); }
    }
}
