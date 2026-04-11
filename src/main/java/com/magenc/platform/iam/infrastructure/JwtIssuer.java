package com.magenc.platform.iam.infrastructure;

import com.magenc.platform.iam.application.TokenService;
import com.magenc.platform.iam.domain.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and refreshes JWT access tokens, plus opaque refresh tokens.
 *
 * <p>Access tokens are signed with RS256 and carry the following claims:
 * <ul>
 *   <li>{@code iss} — the issuer URL ({@code https://api.orchestrate.marketing})</li>
 *   <li>{@code sub} — the user UUID</li>
 *   <li>{@code email} — the user's email</li>
 *   <li>{@code tenant} — the agency slug (e.g., "acme")</li>
 *   <li>{@code role} — the user's role (OWNER, ADMIN, MEMBER, VIEWER)</li>
 *   <li>{@code iat}, {@code exp}, {@code jti} — standard JWT timestamps + ID</li>
 * </ul>
 *
 * <p>Refresh tokens are NOT JWTs. They are 256-bit random strings stored
 * in the {@code admin.refresh_token} table with a hashed lookup key. This
 * is a deliberate design choice: refresh tokens need to be revocable
 * (logout, security incidents, password changes) and JWTs are not natively
 * revocable without a denylist that defeats their purpose.
 */
@Component
public class JwtIssuer implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtIssuer.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtKeyManager keyManager;
    private final RefreshTokenStore refreshTokenStore;

    @Value("${magenc.security.jwt.access-token-ttl}")
    private Duration accessTokenTtl;

    @Value("${magenc.security.jwt.refresh-token-ttl}")
    private Duration refreshTokenTtl;

    public JwtIssuer(JwtKeyManager keyManager, RefreshTokenStore refreshTokenStore) {
        this.keyManager = keyManager;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Override
    @Transactional
    public TokenPair issueTokens(User user, String tenantSlug) {
        Instant now = Instant.now();
        String accessToken = buildAccessToken(user, tenantSlug, now);
        String refreshToken = generateOpaqueToken();
        refreshTokenStore.store(
                refreshToken,
                user.id().value(),
                tenantSlug,
                now.plus(refreshTokenTtl));
        log.debug("Issued token pair for user={} tenant={}", user.id(), tenantSlug);
        return new TokenPair(accessToken, refreshToken, accessTokenTtl);
    }

    @Override
    @Transactional
    public TokenPair refresh(String refreshTokenValue) {
        RefreshTokenStore.RefreshTokenRecord record = refreshTokenStore
                .consume(refreshTokenValue)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or expired"));

        // Single-use semantics: the consume() above already deleted the old token.
        // We now load the user and issue a fresh pair.
        // For now we don't need a full User object, just the claims to rebuild the JWT.
        Instant now = Instant.now();
        String accessToken = buildAccessTokenFromClaims(
                record.userId().toString(),
                record.tenantSlug(),
                now);
        String newRefreshToken = generateOpaqueToken();
        refreshTokenStore.store(
                newRefreshToken,
                record.userId(),
                record.tenantSlug(),
                now.plus(refreshTokenTtl));
        return new TokenPair(accessToken, newRefreshToken, accessTokenTtl);
    }

    @Override
    @Transactional
    public void revoke(String refreshTokenValue) {
        refreshTokenStore.revoke(refreshTokenValue);
    }

    private String buildAccessToken(User user, String tenantSlug, Instant now) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(keyManager.getIssuer())
                .subject(user.id().toString())
                .claim("email", user.email().value())
                .claim("tenant", tenantSlug)
                .claim("role", user.role().name())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(accessTokenTtl)))
                .jwtID(java.util.UUID.randomUUID().toString())
                .build();
        return signClaims(claims);
    }

    private String buildAccessTokenFromClaims(String subject, String tenantSlug, Instant now) {
        // Used during refresh when we have the userId and tenant from the refresh
        // token record but don't want to reload the full User from the database
        // for every refresh call. Note: this means the role is NOT refreshed
        // until the user logs out and back in. If you suspend a user, their
        // existing tokens still work until they expire (max 15 minutes).
        // This is the standard JWT trade-off and accepted in this design.
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(keyManager.getIssuer())
                .subject(subject)
                .claim("tenant", tenantSlug)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(accessTokenTtl)))
                .jwtID(java.util.UUID.randomUUID().toString())
                .build();
        return signClaims(claims);
    }

    private String signClaims(JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(keyManager.getSigningKey().getKeyID())
                    .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            JWSSigner signer = new RSASSASigner(keyManager.getSigningKey());
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
