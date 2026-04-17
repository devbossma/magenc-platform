// Exists because domain SessionRepository interface needs a JPA-backed implementation.
package com.magenc.platform.iam.infrastructure;

import com.magenc.platform.iam.domain.PlatformUserId;
import com.magenc.platform.iam.domain.Session;
import com.magenc.platform.iam.domain.SessionId;
import com.magenc.platform.iam.domain.SessionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaSessionRepository implements SessionRepository {
    private final SpringDataSessionRepository delegate;

    public JpaSessionRepository(SpringDataSessionRepository delegate) {
        this.delegate = delegate;
    }

    @Override public Session save(Session session) {
        Optional<SessionEntity> existing = delegate.findById(session.id().value());
        SessionEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.updateFromDomain(session);
        } else {
            entity = SessionEntity.fromDomain(session);
        }
        return delegate.save(entity).toDomain();
    }

    @Override public Optional<Session> findById(SessionId id) {
        return delegate.findById(id.value()).map(SessionEntity::toDomain);
    }

    @Override @Transactional
    public void revokeAllForUser(PlatformUserId userId, String reason) {
        delegate.revokeAllForUser(userId.value(), reason);
    }

    @Override @Transactional
    public void revokeAllForUserInAgency(PlatformUserId userId, UUID agencyId, String reason) {
        delegate.revokeAllForUserInAgency(userId.value(), agencyId, reason);
    }
}
