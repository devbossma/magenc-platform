-- ============================================================================
-- V2__user_indexes.sql
-- ----------------------------------------------------------------------------
-- Additional indexes on agency_user. The base table was created in
-- V1__baseline_tenant_schema.sql; this adds query-pattern-specific indexes
-- now that we know how the iam module queries it.
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_agency_user_status ON agency_user (status);
CREATE INDEX IF NOT EXISTS idx_agency_user_role ON agency_user (role);
