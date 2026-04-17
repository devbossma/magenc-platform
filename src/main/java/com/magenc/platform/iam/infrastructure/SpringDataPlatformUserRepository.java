// Exists because Spring Data JPA generates the boilerplate SQL we'd otherwise write ourselves.
package com.magenc.platform.iam.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPlatformUserRepository extends JpaRepository<PlatformUserEntity, UUID> {
    Optional<PlatformUserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
