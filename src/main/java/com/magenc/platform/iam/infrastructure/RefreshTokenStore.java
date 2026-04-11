package com.magenc.platform.iam.infrastructure;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores opaque refresh tokens in the admin schema.
 *
 * <p>Refresh tokens live in {@code admin.refresh_token}, NOT in tenant
 * schemas. Two reasons:
 * <ol>
 *   <li>Refresh tokens need to survive even when a tenant schema is dropped
 *       (e.g., for compliance erasure of a tenant's data while keeping the
 *       audit trail of when their session was active)</li>
 *   <li>The refresh endpoint doesn't yet have a tenant context to switch
 *       to until the token is decoded — chicken-and-egg problem solved by
 *       putting the table in admin</li>
 * </ol>
 *
 * <p>The token value is hashed with SHA-256 before storage. We never store
 * the plaintext token. The hash is what we look up. This means a database
 * dump of {@code admin.refresh_token} cannot be used to forge sessions.
 *
 * <p>Uses raw {@link JdbcTemplate} instead of JPA because (a) this table
 * is in admin schema and we don't want to entangle it with the per-tenant
 * Hibernate session, (b) the operations are simple enough that JPA adds
 * no value.
 */
@Component
public class RefreshTokenStore {

    private final JdbcTemplate jdbc;

    public RefreshTokenStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void store(String tokenValue, UUID userId, String tenantSlug, Instant expiresAt) {
        String hash = sha256(tokenValue);
        jdbc.update("""
                INSERT INTO admin.refresh_token (token_hash, user_id, tenant_slug, expires_at, created_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                hash, userId, tenantSlug, java.sql.Timestamp.from(expiresAt),
                java.sql.Timestamp.from(Instant.now()));
    }

    /**
     * Atomically deletes and returns a refresh token record, if it exists
     * and has not expired. Returns empty otherwise.
     *
     * <p>Single-use semantics: a successful consume invalidates the token.
     * The caller must immediately store a new token if they want to keep
     * the session alive.
     */
    @Transactional
    public Optional<RefreshTokenRecord> consume(String tokenValue) {
        String hash = sha256(tokenValue);
        var rows = jdbc.query("""
                DELETE FROM admin.refresh_token
                WHERE token_hash = ? AND expires_at > NOW()
                RETURNING user_id, tenant_slug
                """,
                (rs, rowNum) -> new RefreshTokenRecord(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("tenant_slug")),
                hash);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Transactional
    public void revoke(String tokenValue) {
        String hash = sha256(tokenValue);
        jdbc.update("DELETE FROM admin.refresh_token WHERE token_hash = ?", hash);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RefreshTokenRecord(UUID userId, String tenantSlug) {}
}
