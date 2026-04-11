package com.magenc.platform.iam.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository for {@link UserEntity}.
 *
 * <p>This is the low-level persistence interface used internally by
 * {@link JpaUserRepository}. Application services do not depend on this —
 * they depend on the domain {@code UserRepository} interface.
 */
interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.role = com.magenc.platform.iam.domain.UserRole.OWNER")
    long countOwners();
}
