// Exists because JWT expiry alone cannot revoke sessions: this service manages server-side session state for immediate revocation.
package com.magenc.platform.iam.application;

import com.magenc.platform.iam.domain.PlatformUserId;
import com.magenc.platform.iam.domain.Session;
import com.magenc.platform.iam.domain.SessionId;
import com.magenc.platform.iam.domain.SessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    @Value("${magenc.security.jwt.access-token-ttl}")
    private Duration sessionTtl;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public Session createSession(PlatformUserId userId, UUID agencyId, UUID membershipId,
                                  String userAgent, String ipAddress) {
        Session session = Session.create(userId, agencyId, membershipId,
                Instant.now().plus(sessionTtl), userAgent, ipAddress);
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public boolean isSessionValid(SessionId sessionId) {
        return sessionRepository.findById(sessionId)
                .map(Session::isActive)
                .orElse(false);
    }

    @Transactional
    public void revokeSession(SessionId sessionId, String reason) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.revoke(reason);
            sessionRepository.save(session);
        });
    }

    @Transactional
    public void revokeAllForUser(PlatformUserId userId, String reason) {
        sessionRepository.revokeAllForUser(userId, reason);
    }

    @Transactional
    public void revokeAllForUserInAgency(PlatformUserId userId, UUID agencyId, String reason) {
        sessionRepository.revokeAllForUserInAgency(userId, agencyId, reason);
    }

    @Transactional
    public void touchSession(SessionId sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.touch();
            sessionRepository.save(session);
        });
    }
}
