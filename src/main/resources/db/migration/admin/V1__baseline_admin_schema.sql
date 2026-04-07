-- ============================================================================
-- V1__baseline_admin_schema.sql
-- Admin schema: platform-wide metadata that does NOT belong to any tenant.
-- Holds the catalog of agencies, platform users, billing, audit log.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS admin;

SET search_path TO admin, public;

-- ----------------------------------------------------------------------------
-- agency: the tenant entity
-- ----------------------------------------------------------------------------
CREATE TABLE agency (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            VARCHAR(63)  NOT NULL UNIQUE,
    display_name    VARCHAR(255) NOT NULL,
    plan_tier       VARCHAR(32)  NOT NULL,
    isolation_mode  VARCHAR(32)  NOT NULL,
    region          VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT agency_slug_format CHECK (slug ~ '^[a-z0-9_]+$'),
    CONSTRAINT agency_plan_tier_valid CHECK (plan_tier IN ('STARTER', 'PRO', 'ENTERPRISE')),
    CONSTRAINT agency_isolation_valid CHECK (isolation_mode IN ('SCHEMA', 'DATABASE')),
    CONSTRAINT agency_status_valid CHECK (status IN ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_agency_slug ON agency (slug);
CREATE INDEX idx_agency_status ON agency (status);

-- ----------------------------------------------------------------------------
-- platform_user: Magenc staff (NOT agency users)
-- ----------------------------------------------------------------------------
CREATE TABLE platform_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    role            VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ,

    CONSTRAINT platform_user_role_valid CHECK (role IN ('SUPERADMIN', 'SUPPORT', 'READONLY'))
);

-- ----------------------------------------------------------------------------
-- platform_audit_event: hash-chained audit log for tamper evidence
-- ----------------------------------------------------------------------------
CREATE TABLE platform_audit_event (
    id                BIGSERIAL PRIMARY KEY,
    occurred_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actor_type        VARCHAR(32) NOT NULL,
    actor_id          UUID,
    event_type        VARCHAR(64) NOT NULL,
    target_type       VARCHAR(64),
    target_id         UUID,
    payload           JSONB NOT NULL DEFAULT '{}'::jsonb,
    previous_hash     VARCHAR(64),
    current_hash      VARCHAR(64) NOT NULL,

    CONSTRAINT platform_audit_actor_valid CHECK (actor_type IN ('PLATFORM_USER', 'SYSTEM'))
);

CREATE INDEX idx_platform_audit_occurred ON platform_audit_event (occurred_at DESC);
CREATE INDEX idx_platform_audit_event_type ON platform_audit_event (event_type);

-- ----------------------------------------------------------------------------
-- Spring Modulith JDBC event publication registry
-- ----------------------------------------------------------------------------
CREATE TABLE event_publication (
    id                UUID PRIMARY KEY,
    listener_id       VARCHAR(512) NOT NULL,
    event_type        VARCHAR(512) NOT NULL,
    serialized_event  TEXT NOT NULL,
    publication_date  TIMESTAMPTZ NOT NULL,
    completion_date   TIMESTAMPTZ
);

CREATE INDEX idx_event_publication_serialized ON event_publication (listener_id, serialized_event);
CREATE INDEX idx_event_publication_completion ON event_publication (completion_date);
