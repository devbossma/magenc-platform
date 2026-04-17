// Exists because JWT issuance is a contract the application layer calls without knowing if it's Nimbus, Auth0, or Keycloak behind it.
package com.magenc.platform.iam.application;

import com.magenc.platform.iam.domain.PlatformUser;
import com.magenc.platform.iam.domain.SessionId;
import java.time.Duration;
import java.util.UUID;

public interface TokenService {

    TokenPair issueTokens(PlatformUser user, String tenantSlug, UUID agencyId,
                          UUID membershipId, String role, SessionId sessionId);

    TokenPair refresh(String refreshTokenValue);

    void revoke(String refreshTokenValue);

    record TokenPair(String accessToken, String refreshToken, Duration accessTokenTtl) {}

    class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) { super(message); }
    }
}
