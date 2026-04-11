package com.magenc.platform.iam.domain;

import java.util.UUID;

/**
 * Strongly-typed identifier for a user.
 *
 * <p>Using a value object instead of raw {@code UUID} prevents accidentally
 * passing a {@code ClientId} or {@code AgencyId} where a {@code UserId}
 * is expected. The compiler catches what tests cannot.
 */
public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("UserId value cannot be null");
        }
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId fromString(String value) {
        return new UserId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
