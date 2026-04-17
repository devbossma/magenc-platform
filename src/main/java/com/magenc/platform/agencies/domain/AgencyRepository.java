// Exists because domain defines what queries are needed; infrastructure decides how (JPA, JDBC, etc).
package com.magenc.platform.agencies.domain;

import java.util.Optional;

public interface AgencyRepository {
    Agency save(Agency agency);
    Optional<Agency> findById(AgencyId id);
    Optional<Agency> findBySlug(AgencySlug slug);
    boolean existsBySlug(AgencySlug slug);
    void deleteById(AgencyId id);
}
