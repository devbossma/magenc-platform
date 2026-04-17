package com.magenc.platform.agencies.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAgencyRepository extends JpaRepository<AgencyEntity, UUID> {
    Optional<AgencyEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
