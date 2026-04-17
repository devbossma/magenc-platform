// Exists because session persistence is an infrastructure concern; the domain defines what operations are needed.
package com.magenc.platform.iam.domain;

import java.util.Optional;

public interface SessionRepository {
    Session save(Session session);
    Optional<Session> findById(SessionId id);
    void revokeAllForUser(PlatformUserId userId, String reason);
    void revokeAllForUserInAgency(PlatformUserId userId, java.util.UUID agencyId, String reason);
}
