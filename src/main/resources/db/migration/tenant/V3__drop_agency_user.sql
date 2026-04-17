-- ============================================================================
-- V3__drop_agency_user.sql
-- Model B: user identity moved to admin.platform_user + admin.agency_membership
-- The agency_user table in tenant schemas is no longer needed.
-- ============================================================================

DROP TABLE IF EXISTS agency_user CASCADE;
