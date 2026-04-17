// Exists because domain AgencyMembershipRepository needs a JPA implementation.
package com.magenc.platform.agencies.infrastructure;

import com.magenc.platform.agencies.domain.AgencyId;
import com.magenc.platform.agencies.domain.AgencyMembership;
import com.magenc.platform.agencies.domain.AgencyMembershipRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaAgencyMembershipRepository implements AgencyMembershipRepository {
    private final SpringDataAgencyMembershipRepository delegate;
    public JpaAgencyMembershipRepository(SpringDataAgencyMembershipRepository delegate) { this.delegate = delegate; }

    @Override public AgencyMembership save(AgencyMembership m) {
        return delegate.save(AgencyMembershipEntity.fromDomain(m)).toDomain();
    }

    @Override public Optional<AgencyMembership> findByUserAndAgency(UUID platformUserId, AgencyId agencyId) {
        return delegate.findByPlatformUserIdAndAgencyId(platformUserId, agencyId.value())
                .map(AgencyMembershipEntity::toDomain);
    }

    @Override public void deleteById(UUID id) {
        delegate.deleteById(id);
    }
}
