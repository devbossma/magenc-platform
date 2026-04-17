-- ============================================================================
-- V4__fix_agency_plan_tier_constraint.sql
-- Fix the agency plan_tier constraint to include TRIAL tier
-- ============================================================================

ALTER TABLE admin.agency DROP CONSTRAINT agency_plan_tier_valid;

ALTER TABLE admin.agency ADD CONSTRAINT agency_plan_tier_valid CHECK (
    plan_tier IN ('TRIAL', 'STARTER', 'PRO', 'ENTERPRISE')
);
