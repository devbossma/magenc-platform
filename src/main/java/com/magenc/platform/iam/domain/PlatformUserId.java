// Exists because raw UUIDs are stringly-typed: this prevents mixing up user, agency, and client IDs at compile time.
package com.magenc.platform.iam.domain;

import java.util.UUID;

public record PlatformUserId(UUID value) {

    public PlatformUserId {
        if (value == null) throw new IllegalArgumentException("PlatformUserId cannot be null");
    }

    public static PlatformUserId generate() { return new PlatformUserId(UUID.randomUUID()); }
    public static PlatformUserId of(UUID value) { return new PlatformUserId(value); }
    public static PlatformUserId fromString(String value) { return new PlatformUserId(UUID.fromString(value)); }

    @Override public String toString() { return value.toString(); }
}
