// Exists because this is the only class that touches the RSA private key to sign JWTs: centralizing signing prevents key leaks.
package com.magenc.platform.iam.infrastructure;

import com.magenc.platform.iam.application.TokenService;
import com.magenc.platform.iam.domain.PlatformUser;
import com.magenc.platform.iam.domain.SessionId;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Override @Transactional
    public TokenPair issueTokens(PlatformUser user, String tenantSlug, UUID agencyId,
                                  UUID membershipId, String role, SessionId sessionId) {
        Instant now = Instant.now();
        String accessToken = buildAccessToken(user, tenantSlug, agencyId, membershipId, role, sessionId, now);
        String refreshToken = generateOpaqueToken();
        refreshTokenStore.store(refreshToken, user.id().value(), tenantSlug, now.plus(refreshTokenTtl));
        log.debug("Issued token pair for user={} tenant={}", user.id(), tenantSlug);
        return new TokenPair(accessToken, refreshToken, accessTokenTtl);
    }

    @Override @Transactional
    public TokenPair refresh(String refreshTokenValue) {
        RefreshTokenStore.RefreshTokenRecord record = refreshTokenStore
                .consume(refreshTokenValue)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or expired"));
        Instant now = Instant.now();
        // Minimal JWT for refresh: no full user reload, role not refreshed until re-login
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(keyManager.getIssuer())
                .subject(record.userId().toString())
                .claim("tenant", record.tenantSlug())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(accessTokenTtl)))
                .jwtID(UUID.randomUUID().toString())
                .build();
        String accessToken = signClaims(claims);
        String newRefreshToken = generateOpaqueToken();
        refreshTokenStore.store(newRefreshToken, record.userId(), record.tenantSlug(), now.plus(refreshTokenTtl));
        return new TokenPair(accessToken, newRefreshToken, accessTokenTtl);
    }

    @Override @Transactional
    public void revoke(String refreshTokenValue) {
        refreshTokenStore.revoke(refreshTokenValue);
    }

    private String buildAccessToken(PlatformUser user, String tenantSlug, UUID agencyId,
                                     UUID membershipId, String role, SessionId sessionId, Instant now) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(keyManager.getIssuer())
                .subject(user.id().toString())
                .claim("email", user.email().value())
                .claim("display_name", user.displayName())
                .claim("tenant", tenantSlug)
                .claim("agency_id", agencyId.toString())
                .claim("membership_id", membershipId.toString())
                .claim("role", role)
                .claim("sid", sessionId.toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(accessTokenTtl)))
                .jwtID(sessionId.toString())
                .build();
        return signClaims(claims);
    }

    private String signClaims(JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(keyManager.getSigningKey().getKeyID()).build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new RSASSASigner(keyManager.getSigningKey()));
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
