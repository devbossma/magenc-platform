// Exists because the membership is the link between a global user and a specific agency with a specific role.
package com.magenc.platform.agencies.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public final class AgencyMembership {

    private final UUID id;
    private final UUID platformUserId;
    private final AgencyId agencyId;
    private MembershipRole role;
    private MembershipStatus status;
    private @Nullable UUID invitedBy;
    private final Instant joinedAt;

    public static AgencyMembership createOwner(UUID platformUserId, AgencyId agencyId) {
        Objects.requireNonNull(platformUserId);
        Objects.requireNonNull(agencyId);
        return new AgencyMembership(UUID.randomUUID(), platformUserId, agencyId,
                MembershipRole.OWNER, MembershipStatus.ACTIVE, null, Instant.now());
    }

    public static AgencyMembership reconstitute(
            UUID id, UUID platformUserId, AgencyId agencyId, MembershipRole role,
            MembershipStatus status, @Nullable UUID invitedBy, Instant joinedAt) {
        return new AgencyMembership(id, platformUserId, agencyId, role, status, invitedBy, joinedAt);
    }

    private AgencyMembership(UUID id, UUID platformUserId, AgencyId agencyId,
                              MembershipRole role, MembershipStatus status,
                              @Nullable UUID invitedBy, Instant joinedAt) {
        this.id = id; this.platformUserId = platformUserId; this.agencyId = agencyId;
        this.role = role; this.status = status; this.invitedBy = invitedBy; this.joinedAt = joinedAt;
    }

    public void remove() { this.status = MembershipStatus.REMOVED; }
    public boolean isActive() { return status == MembershipStatus.ACTIVE; }

    public UUID id() { return id; }
    public UUID platformUserId() { return platformUserId; }
    public AgencyId agencyId() { return agencyId; }
    public MembershipRole role() { return role; }
    public MembershipStatus status() { return status; }
    public @Nullable UUID invitedBy() { return invitedBy; }
    public Instant joinedAt() { return joinedAt; }
}
