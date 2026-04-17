// Exists because raw UUIDs leak: this typed wrapper prevents passing a UserId where an AgencyId is expected.
package com.magenc.platform.agencies.domain;

import java.util.UUID;

public record AgencyId(UUID value) {
    public AgencyId { if (value == null) throw new IllegalArgumentException("AgencyId cannot be null"); }
    public static AgencyId generate() { return new AgencyId(UUID.randomUUID()); }
    public static AgencyId of(UUID value) { return new AgencyId(value); }
    @Override public String toString() { return value.toString(); }
}
