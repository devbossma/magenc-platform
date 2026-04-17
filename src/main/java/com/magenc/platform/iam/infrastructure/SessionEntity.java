// Exists because sessions need JPA persistence: this entity maps to admin.session for server-side revocation tracking.
package com.magenc.platform.iam.infrastructure;

import com.magenc.platform.iam.domain.PlatformUserId;
import com.magenc.platform.iam.domain.Session;
import com.magenc.platform.iam.domain.SessionId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session", schema = "admin")
public class SessionEntity {

    @Id @Column(name = "id") private UUID id;
    @Column(name = "platform_user_id", nullable = false) private UUID platformUserId;
    @Column(name = "agency_id", nullable = false) private UUID agencyId;
    @Column(name = "membership_id", nullable = false) private UUID membershipId;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;
    @Column(name = "last_seen_at", nullable = false) private Instant lastSeenAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "revoke_reason", length = 64) private String revokeReason;
    @Column(name = "user_agent", columnDefinition = "TEXT") private String userAgent;
    @Column(name = "ip_address", columnDefinition = "INET") private String ipAddress;

    protected SessionEntity() {}

    public static SessionEntity fromDomain(Session s) {
        var e = new SessionEntity();
        e.id = s.id().value();
        e.platformUserId = s.userId().value();
        e.agencyId = s.agencyId();
        e.membershipId = s.membershipId();
        e.issuedAt = s.issuedAt();
        e.lastSeenAt = s.lastSeenAt();
        e.expiresAt = s.expiresAt();
        e.revokedAt = s.revokedAt();
        e.revokeReason = s.revokeReason();
        e.userAgent = s.userAgent();
        e.ipAddress = s.ipAddress();
        return e;
    }

    public Session toDomain() {
        return Session.reconstitute(
                SessionId.of(id), PlatformUserId.of(platformUserId),
                agencyId, membershipId, issuedAt, lastSeenAt, expiresAt,
                revokedAt, revokeReason, userAgent, ipAddress);
    }

    public void updateFromDomain(Session s) {
        this.lastSeenAt = s.lastSeenAt();
        this.revokedAt = s.revokedAt();
        this.revokeReason = s.revokeReason();
    }

    public UUID getId() { return id; }
}
