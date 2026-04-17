-- ============================================================================
-- V3__model_b_identity.sql
-- Model B refactor: global platform_user + agency_membership + sessions + auth audit
-- ============================================================================

-- Platform users: global identity, not tenant-scoped
-- Rename old platform_user to preserve data, then create the new schema
ALTER TABLE admin.platform_user RENAME TO platform_user_v1;

CREATE TABLE admin.platform_user (
    id                  UUID PRIMARY KEY,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255),
    display_name        VARCHAR(255) NOT NULL,
    email_verified_at   TIMESTAMPTZ,
    status              VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    sso_provider        VARCHAR(64),
    sso_subject         VARCHAR(255),
    mfa_secret          VARCHAR(255),
    mfa_enabled_at      TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at       TIMESTAMPTZ,
    CONSTRAINT pu_status_valid CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

-- Migrate data from old table to new table
INSERT INTO admin.platform_user (id, email, password_hash, display_name, created_at, last_login_at)
SELECT id, email, password_hash, display_name, created_at, last_login_at
FROM admin.platform_user_v1;

-- Drop the old table
DROP TABLE admin.platform_user_v1;

CREATE INDEX idx_pu_email ON admin.platform_user (email);
CREATE INDEX idx_pu_status ON admin.platform_user (status);

-- Add enterprise hooks to existing agency table
ALTER TABLE admin.agency
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS trial_ends_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS activated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS sso_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sso_config JSONB,
    ADD COLUMN IF NOT EXISTS ip_allowlist JSONB;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'agency_payment_valid'
                   AND table_schema = 'admin') THEN
        ALTER TABLE admin.agency ADD CONSTRAINT agency_payment_valid CHECK (
            payment_status IN ('NONE', 'SIMULATED_PAID', 'STRIPE_ACTIVE', 'STRIPE_PAST_DUE')
        );
    END IF;
END $$;

-- Agency memberships: who can access what with what role
CREATE TABLE admin.agency_membership (
    id                  UUID PRIMARY KEY,
    platform_user_id    UUID NOT NULL REFERENCES admin.platform_user(id) ON DELETE CASCADE,
    agency_id           UUID NOT NULL REFERENCES admin.agency(id) ON DELETE CASCADE,
    role                VARCHAR(32) NOT NULL,
    status              VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    invited_by          UUID REFERENCES admin.platform_user(id),
    joined_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (platform_user_id, agency_id),
    CONSTRAINT am_role_valid CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER')),
    CONSTRAINT am_status_valid CHECK (status IN ('ACTIVE', 'INVITED', 'REMOVED'))
);

CREATE INDEX idx_am_user ON admin.agency_membership (platform_user_id);
CREATE INDEX idx_am_agency ON admin.agency_membership (agency_id);
CREATE INDEX idx_am_user_status ON admin.agency_membership (platform_user_id, status);

-- Sessions: server-side tracking for revocation
CREATE TABLE admin.session (
    id                  UUID PRIMARY KEY,
    platform_user_id    UUID NOT NULL REFERENCES admin.platform_user(id) ON DELETE CASCADE,
    agency_id           UUID NOT NULL REFERENCES admin.agency(id) ON DELETE CASCADE,
    membership_id       UUID NOT NULL REFERENCES admin.agency_membership(id),
    issued_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ NOT NULL,
    revoked_at          TIMESTAMPTZ,
    revoke_reason       VARCHAR(64),
    user_agent          TEXT,
    ip_address          VARCHAR(45)
);

CREATE INDEX idx_session_user ON admin.session (platform_user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_session_agency ON admin.session (agency_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_session_expires ON admin.session (expires_at);

-- Auth event audit log with hash chaining
CREATE TABLE admin.auth_event (
    id                  BIGSERIAL PRIMARY KEY,
    occurred_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    event_type          VARCHAR(64) NOT NULL,
    platform_user_id    UUID,
    agency_id           UUID,
    session_id          UUID,
    ip_address          VARCHAR(45),
    user_agent          TEXT,
    success             BOOLEAN NOT NULL,
    failure_reason      VARCHAR(128),
    payload             JSONB NOT NULL DEFAULT '{}'::jsonb,
    previous_hash       VARCHAR(64),
    current_hash        VARCHAR(64) NOT NULL
);

CREATE INDEX idx_auth_event_occurred ON admin.auth_event (occurred_at DESC);
CREATE INDEX idx_auth_event_user ON admin.auth_event (platform_user_id);
CREATE INDEX idx_auth_event_agency ON admin.auth_event (agency_id);
CREATE INDEX idx_auth_event_type ON admin.auth_event (event_type);

-- Permissions: empty table ready for per-resource grants (Phase 2)
CREATE TABLE admin.permission (
    id                  UUID PRIMARY KEY,
    membership_id       UUID NOT NULL REFERENCES admin.agency_membership(id) ON DELETE CASCADE,
    resource_type       VARCHAR(64) NOT NULL,
    resource_id         VARCHAR(128),
    action              VARCHAR(32) NOT NULL,
    granted_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by          UUID REFERENCES admin.platform_user(id)
);

CREATE INDEX idx_perm_membership ON admin.permission (membership_id);

-- Signup attempt tracking for orphan recovery
CREATE TABLE admin.signup_attempt (
    id                  UUID PRIMARY KEY,
    email               VARCHAR(255) NOT NULL,
    tenant_slug         VARCHAR(63) NOT NULL,
    status              VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    last_completed_step VARCHAR(64),
    platform_user_id    UUID,
    agency_id           UUID,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,
    failed_at           TIMESTAMPTZ,
    failure_reason      TEXT,
    CONSTRAINT sa_status_valid CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'ROLLED_BACK'))
);

CREATE INDEX idx_sa_status ON admin.signup_attempt (status);
CREATE INDEX idx_sa_started ON admin.signup_attempt (started_at);
