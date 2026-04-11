package com.magenc.platform.iam.domain;

import java.util.regex.Pattern;

/**
 * A raw, unhashed password as entered by the user.
 *
 * <p>This type exists to enforce password policy at the type system level —
 * any {@code RawPassword} instance is guaranteed to satisfy the policy.
 *
 * <p><b>Critical handling rules:</b>
 * <ul>
 *   <li>NEVER log a {@code RawPassword}. The {@link #toString()} method
 *       returns a redacted placeholder, and frameworks like SLF4J will
 *       use that.</li>
 *   <li>NEVER serialize a {@code RawPassword} to JSON or any persistent
 *       format. There are no Jackson annotations, but be vigilant.</li>
 *   <li>Convert to {@link HashedPassword} via {@link PasswordHasher} as
 *       early as possible after construction.</li>
 *   <li>The {@link #value()} accessor exists for hasher implementations
 *       only. Treat it like a JNI hot path: respect it.</li>
 * </ul>
 *
 * <p><b>Policy:</b>
 * <ul>
 *   <li>Minimum 12 characters</li>
 *   <li>Maximum 128 characters (defense against bcrypt DoS)</li>
 *   <li>At least one uppercase letter</li>
 *   <li>At least one lowercase letter</li>
 *   <li>At least one digit</li>
 *   <li>At least one symbol from a permissive set</li>
 * </ul>
 */
public final class RawPassword {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9]");

    private final String value;

    private RawPassword(String value) {
        this.value = value;
    }

    public static RawPassword of(String value) {
        if (value == null) {
            throw new WeakPasswordException("Password cannot be null");
        }
        if (value.length() < MIN_LENGTH) {
            throw new WeakPasswordException(
                    "Password must be at least " + MIN_LENGTH + " characters");
        }
        if (value.length() > MAX_LENGTH) {
            throw new WeakPasswordException(
                    "Password must be at most " + MAX_LENGTH + " characters");
        }
        if (!UPPERCASE.matcher(value).find()) {
            throw new WeakPasswordException(
                    "Password must contain at least one uppercase letter");
        }
        if (!LOWERCASE.matcher(value).find()) {
            throw new WeakPasswordException(
                    "Password must contain at least one lowercase letter");
        }
        if (!DIGIT.matcher(value).find()) {
            throw new WeakPasswordException(
                    "Password must contain at least one digit");
        }
        if (!SYMBOL.matcher(value).find()) {
            throw new WeakPasswordException(
                    "Password must contain at least one symbol");
        }
        return new RawPassword(value);
    }

    /**
     * Returns the raw password value.
     *
     * <p><b>Only for use by {@link PasswordHasher} implementations.</b>
     * Calling this from any other code is a code smell and should be
     * caught in code review.
     */
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "RawPassword[REDACTED]";
    }

    public static class WeakPasswordException extends RuntimeException {
        public WeakPasswordException(String message) {
            super(message);
        }
    }
}
