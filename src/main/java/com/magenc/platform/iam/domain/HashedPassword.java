// Exists because bcrypt hashes have a known format: this type validates the format so downstream code trusts it.
package com.magenc.platform.iam.domain;

public record HashedPassword(String value) {
    public HashedPassword {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Hashed password cannot be blank");
        if (!value.startsWith("$2") || value.length() != 60) throw new IllegalArgumentException("Not a bcrypt hash");
    }
}
