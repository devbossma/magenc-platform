// Exists because the membership join table needs JPA mapping for admin.agency_membership.
package com.magenc.platform.agencies.infrastructure;

import com.magenc.platform.agencies.domain.AgencyId;
import com.magenc.platform.agencies.domain.AgencyMembership;
import com.magenc.platform.agencies.domain.MembershipRole;
import com.magenc.platform.agencies.domain.MembershipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agency_membership", schema = "admin")
public class AgencyMembershipEntity {

    @Id @Column(name = "id") private UUID id;
    @Column(name = "platform_user_id", nullable = false) private UUID platformUserId;
    @Column(name = "agency_id", nullable = false) private UUID agencyId;
    @Enumerated(EnumType.STRING) @Column(name = "role", nullable = false, length = 32) private MembershipRole role;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 32) private MembershipStatus status;
    @Column(name = "invited_by") private UUID invitedBy;
    @Column(name = "joined_at", nullable = false) private Instant joinedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected AgencyMembershipEntity() {}

    public static AgencyMembershipEntity fromDomain(AgencyMembership m) {
        var e = new AgencyMembershipEntity();
        e.id = m.id(); e.platformUserId = m.platformUserId(); e.agencyId = m.agencyId().value();
        e.role = m.role(); e.status = m.status(); e.invitedBy = m.invitedBy();
        e.joinedAt = m.joinedAt(); e.updatedAt = Instant.now();
        return e;
    }

    public AgencyMembership toDomain() {
        return AgencyMembership.reconstitute(id, platformUserId, AgencyId.of(agencyId),
                role, status, invitedBy, joinedAt);
    }

    public UUID getId() { return id; }
}
