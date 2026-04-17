// Exists because Spring Data generates the session queries; the adapter converts to domain types.
package com.magenc.platform.iam.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface SpringDataSessionRepository extends JpaRepository<SessionEntity, UUID> {
    Optional<SessionEntity> findById(UUID id);

    @Modifying
    @Query("UPDATE SessionEntity s SET s.revokedAt = CURRENT_TIMESTAMP, s.revokeReason = :reason WHERE s.platformUserId = :userId AND s.revokedAt IS NULL")
    void revokeAllForUser(UUID userId, String reason);

    @Modifying
    @Query("UPDATE SessionEntity s SET s.revokedAt = CURRENT_TIMESTAMP, s.revokeReason = :reason WHERE s.platformUserId = :userId AND s.agencyId = :agencyId AND s.revokedAt IS NULL")
    void revokeAllForUserInAgency(UUID userId, UUID agencyId, String reason);
}
