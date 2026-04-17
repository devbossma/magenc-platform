// Exists because session IDs travel through JWTs and filters: a typed wrapper prevents mix-ups with user/agency IDs.
package com.magenc.platform.iam.domain;

import java.util.UUID;

public record SessionId(UUID value) {
    public SessionId {
        if (value == null) throw new IllegalArgumentException("SessionId cannot be null");
    }
    public static SessionId generate() { return new SessionId(UUID.randomUUID()); }
    public static SessionId of(UUID value) { return new SessionId(value); }
    public static SessionId fromString(String value) { return new SessionId(UUID.fromString(value)); }
    @Override public String toString() { return value.toString(); }
}
