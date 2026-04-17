// Exists because any Email instance is guaranteed valid: downstream code never needs to re-check format.
package com.magenc.platform.iam.domain;

import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final int MAX_LENGTH = 255;

    public Email {
        if (value == null || value.isBlank()) throw new InvalidEmailException("Email cannot be blank");
        String normalized = value.trim().toLowerCase();
        if (normalized.length() > MAX_LENGTH) throw new InvalidEmailException("Email exceeds max length");
        if (!EMAIL_PATTERN.matcher(normalized).matches()) throw new InvalidEmailException("Invalid email format: " + value);
        value = normalized;
    }

    public static Email of(String value) { return new Email(value); }
    @Override public String toString() { return value; }

    public static class InvalidEmailException extends RuntimeException {
        public InvalidEmailException(String message) { super(message); }
    }
}
