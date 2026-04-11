package com.magenc.platform.iam.domain;

import java.util.regex.Pattern;

/**
 * Validated email address value object.
 *
 * <p>Validation happens at construction time, so any {@code Email} instance
 * in the system is guaranteed to be syntactically valid. Code downstream
 * doesn't need to re-check.
 *
 * <p>The regex is intentionally permissive (RFC 5322 compliance is overkill
 * for our purposes). We accept anything that has a local part, an at-sign,
 * a domain part, a dot, and a TLD of at least 2 characters.
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final int MAX_LENGTH = 255;

    public Email {
        if (value == null || value.isBlank()) {
            throw new InvalidEmailException("Email cannot be blank");
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.length() > MAX_LENGTH) {
            throw new InvalidEmailException("Email exceeds maximum length of " + MAX_LENGTH);
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidEmailException("Invalid email format: " + value);
        }
        value = normalized;
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }

    public static class InvalidEmailException extends RuntimeException {
        public InvalidEmailException(String message) {
            super(message);
        }
    }
}
