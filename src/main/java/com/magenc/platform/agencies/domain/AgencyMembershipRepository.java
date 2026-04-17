// Exists because membership queries are the core of Model B access control: who can access what agency with what role.
package com.magenc.platform.agencies.domain;

import java.util.Optional;
import java.util.UUID;

public interface AgencyMembershipRepository {
    AgencyMembership save(AgencyMembership membership);
    Optional<AgencyMembership> findByUserAndAgency(UUID platformUserId, AgencyId agencyId);
    void deleteById(UUID id);
}
