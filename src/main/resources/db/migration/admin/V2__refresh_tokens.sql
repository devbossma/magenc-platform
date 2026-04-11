-- ============================================================================
-- V2__refresh_tokens.sql
-- ----------------------------------------------------------------------------
-- Refresh token store. Lives in admin (NOT per tenant) so the table
-- survives tenant schema deletion and is reachable before the JWT-based
-- tenant resolution has happened (refresh endpoint is unauthenticated).
--
-- Tokens are stored as SHA-256 hashes, NEVER as plaintext. The plaintext
-- only ever exists in memory in the issuer + the client cookie.
-- ============================================================================

CREATE TABLE admin.refresh_token (
    token_hash      VARCHAR(64)  PRIMARY KEY,
    user_id         UUID         NOT NULL,
    tenant_slug     VARCHAR(63)  NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_token_user ON admin.refresh_token (user_id);
CREATE INDEX idx_refresh_token_tenant ON admin.refresh_token (tenant_slug);
CREATE INDEX idx_refresh_token_expires ON admin.refresh_token (expires_at);

-- Periodic cleanup of expired tokens runs nightly via a scheduled job
-- (will add when we wire up the scheduler module). Until then they
-- accumulate harmlessly; consume() filters by expiry already.

COMMENT ON TABLE admin.refresh_token IS
    'Opaque refresh tokens hashed with SHA-256. Single-use semantics enforced by DELETE...RETURNING.';
