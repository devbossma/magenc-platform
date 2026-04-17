package com.magenc.platform.agencies.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAgencyMembershipRepository extends JpaRepository<AgencyMembershipEntity, UUID> {
    Optional<AgencyMembershipEntity> findByPlatformUserIdAndAgencyId(UUID platformUserId, UUID agencyId);
}
