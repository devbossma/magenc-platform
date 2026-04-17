// Exists because domain AgencyRepository needs a JPA implementation that converts between domain and entity types.
package com.magenc.platform.agencies.infrastructure;

import com.magenc.platform.agencies.domain.Agency;
import com.magenc.platform.agencies.domain.AgencyId;
import com.magenc.platform.agencies.domain.AgencyRepository;
import com.magenc.platform.agencies.domain.AgencySlug;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaAgencyRepository implements AgencyRepository {
    private final SpringDataAgencyRepository delegate;
    public JpaAgencyRepository(SpringDataAgencyRepository delegate) { this.delegate = delegate; }

    @Override public Agency save(Agency agency) {
        Optional<AgencyEntity> existing = delegate.findById(agency.id().value());
        AgencyEntity entity;
        if (existing.isPresent()) { entity = existing.get(); entity.updateFromDomain(agency); }
        else { entity = AgencyEntity.fromDomain(agency); }
        return delegate.save(entity).toDomain();
    }

    @Override public Optional<Agency> findById(AgencyId id) {
        return delegate.findById(id.value()).map(AgencyEntity::toDomain);
    }

    @Override public Optional<Agency> findBySlug(AgencySlug slug) {
        return delegate.findBySlug(slug.value()).map(AgencyEntity::toDomain);
    }

    @Override public boolean existsBySlug(AgencySlug slug) {
        return delegate.existsBySlug(slug.value());
    }

    @Override public void deleteById(AgencyId id) {
        delegate.deleteById(id.value());
    }
}
