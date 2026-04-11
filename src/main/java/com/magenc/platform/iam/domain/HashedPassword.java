package com.magenc.platform.iam.domain;

/**
 * A bcrypt-hashed password ready for storage or comparison.
 *
 * <p>Unlike {@link RawPassword}, this type IS safe to store in the database,
 * pass through service layers, and serialize. The hash is one-way: no code
 * path in the system can reconstruct the original password from this value.
 *
 * <p>Hashes are produced by {@link PasswordHasher#hash(RawPassword)} and
 * verified by {@link PasswordHasher#matches(RawPassword, HashedPassword)}.
 */
public record HashedPassword(String value) {

    public HashedPassword {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hashed password cannot be blank");
        }
        // bcrypt hashes always start with $2a$, $2b$, or $2y$ and are 60 chars
        if (!value.startsWith("$2") || value.length() != 60) {
            throw new IllegalArgumentException(
                    "Value does not look like a bcrypt hash");
        }
    }
}
