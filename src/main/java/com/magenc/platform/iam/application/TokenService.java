package com.magenc.platform.iam.application;

import com.magenc.platform.iam.domain.User;
import java.time.Duration;

/**
 * Issues and validates authentication tokens.
 *
 * <p>This is the seam between the auth use cases and the actual JWT
 * implementation. The application layer talks to this interface; the
 * infrastructure layer provides a JWT-backed implementation.
 *
 * <p>If we ever migrate from hand-rolled JWTs to Spring Authorization
 * Server, Auth0, or Keycloak, only the implementation changes — every
 * other class in this module is unaffected.
 */
public interface TokenService {

    /**
     * Issues a fresh token pair for the given user within the given tenant.
     *
     * @param user        the authenticated user
     * @param tenantSlug  the agency the user belongs to (lives in the JWT
     *                    {@code tenant} claim, used for downstream routing)
     */
    TokenPair issueTokens(User user, String tenantSlug);

    /**
     * Exchanges a valid refresh token for a new token pair.
     *
     * @throws InvalidTokenException if the refresh token is unknown,
     *                               revoked, expired, or has already
     *                               been used (single-use semantics)
     */
    TokenPair refresh(String refreshTokenValue);

    /**
     * Revokes a refresh token. Used during logout.
     *
     * <p>Idempotent: revoking an unknown or already-revoked token is a no-op.
     */
    void revoke(String refreshTokenValue);

    record TokenPair(
            String accessToken,
            String refreshToken,
            Duration accessTokenTtl
    ) {}

    class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
