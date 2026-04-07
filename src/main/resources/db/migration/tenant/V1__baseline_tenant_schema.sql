-- ============================================================================
-- V1__baseline_tenant_schema.sql
-- TENANT schema baseline. Run with search_path already set to the new
-- tenant's schema by TenantProvisioningService.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- agency_user: people who work at this specific agency
-- ----------------------------------------------------------------------------
CREATE TABLE agency_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    role            VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ,

    CONSTRAINT agency_user_role_valid CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER')),
    CONSTRAINT agency_user_status_valid CHECK (status IN ('ACTIVE', 'INVITED', 'SUSPENDED'))
);

-- ----------------------------------------------------------------------------
-- client: an end-customer of the agency
-- ----------------------------------------------------------------------------
CREATE TABLE client (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    client_type     VARCHAR(64)  NOT NULL,
    industry        VARCHAR(128),
    brand_profile   JSONB NOT NULL DEFAULT '{}'::jsonb,
    status          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT client_type_valid CHECK (client_type IN ('SAAS', 'ECOMMERCE', 'PERSONAL_BRAND', 'PHYSICAL_PRODUCT', 'OTHER')),
    CONSTRAINT client_status_valid CHECK (status IN ('ACTIVE', 'PAUSED', 'ARCHIVED'))
);

CREATE INDEX idx_client_status ON client (status);
CREATE INDEX idx_client_type ON client (client_type);

-- ----------------------------------------------------------------------------
-- llm_credential: encrypted BYOK API keys
-- ----------------------------------------------------------------------------
CREATE TABLE llm_credential (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider            VARCHAR(64) NOT NULL,
    label               VARCHAR(128) NOT NULL,
    encrypted_key       BYTEA NOT NULL,
    encrypted_dek       BYTEA NOT NULL,
    key_fingerprint     VARCHAR(64) NOT NULL,
    status              VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_used_at        TIMESTAMPTZ,

    CONSTRAINT llm_credential_status_valid CHECK (status IN ('ACTIVE', 'REVOKED'))
);

-- ----------------------------------------------------------------------------
-- audit_event: per-tenant hash-chained audit log
-- ----------------------------------------------------------------------------
CREATE TABLE audit_event (
    id                BIGSERIAL PRIMARY KEY,
    occurred_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actor_id          UUID,
    actor_email       VARCHAR(255),
    event_type        VARCHAR(64) NOT NULL,
    target_type       VARCHAR(64),
    target_id         VARCHAR(128),
    payload           JSONB NOT NULL DEFAULT '{}'::jsonb,
    previous_hash     VARCHAR(64),
    current_hash      VARCHAR(64) NOT NULL
);

CREATE INDEX idx_audit_occurred ON audit_event (occurred_at DESC);
CREATE INDEX idx_audit_event_type ON audit_event (event_type);
