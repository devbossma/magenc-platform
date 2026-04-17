// Exists because JWTs alone cannot be revoked: server-side sessions let us invalidate compromised tokens immediately.
package com.magenc.platform.iam.domain;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public final class Session {

    private final SessionId id;
    private final PlatformUserId userId;
    private final UUID agencyId;
    private final UUID membershipId;
    private final Instant issuedAt;
    private Instant lastSeenAt;
    private final Instant expiresAt;
    private @Nullable Instant revokedAt;
    private @Nullable String revokeReason;
    private @Nullable String userAgent;
    private @Nullable String ipAddress;

    public static Session create(
            PlatformUserId userId, UUID agencyId, UUID membershipId,
            Instant expiresAt, @Nullable String userAgent, @Nullable String ipAddress) {
        Instant now = Instant.now();
        return new Session(SessionId.generate(), userId, agencyId, membershipId,
                now, now, expiresAt, null, null, userAgent, ipAddress);
    }

    public static Session reconstitute(
            SessionId id, PlatformUserId userId, UUID agencyId, UUID membershipId,
            Instant issuedAt, Instant lastSeenAt, Instant expiresAt,
            @Nullable Instant revokedAt, @Nullable String revokeReason,
            @Nullable String userAgent, @Nullable String ipAddress) {
        return new Session(id, userId, agencyId, membershipId, issuedAt, lastSeenAt,
                expiresAt, revokedAt, revokeReason, userAgent, ipAddress);
    }

    private Session(SessionId id, PlatformUserId userId, UUID agencyId, UUID membershipId,
                    Instant issuedAt, Instant lastSeenAt, Instant expiresAt,
                    @Nullable Instant revokedAt, @Nullable String revokeReason,
                    @Nullable String userAgent, @Nullable String ipAddress) {
        this.id = id;
        this.userId = userId;
        this.agencyId = agencyId;
        this.membershipId = membershipId;
        this.issuedAt = issuedAt;
        this.lastSeenAt = lastSeenAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.revokeReason = revokeReason;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    public boolean isActive() {
        return revokedAt == null && Instant.now().isBefore(expiresAt);
    }

    public void revoke(String reason) {
        if (revokedAt != null) return;
        this.revokedAt = Instant.now();
        this.revokeReason = reason;
    }

    public void touch() { this.lastSeenAt = Instant.now(); }

    public SessionId id() { return id; }
    public PlatformUserId userId() { return userId; }
    public UUID agencyId() { return agencyId; }
    public UUID membershipId() { return membershipId; }
    public Instant issuedAt() { return issuedAt; }
    public Instant lastSeenAt() { return lastSeenAt; }
    public Instant expiresAt() { return expiresAt; }
    public @Nullable Instant revokedAt() { return revokedAt; }
    public @Nullable String revokeReason() { return revokeReason; }
    public @Nullable String userAgent() { return userAgent; }
    public @Nullable String ipAddress() { return ipAddress; }
}
