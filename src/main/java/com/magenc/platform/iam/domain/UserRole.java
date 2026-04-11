package com.magenc.platform.iam.domain;

/**
 * The role of a user within their agency tenant.
 *
 * <p>Roles are tenant-scoped, not platform-scoped. An OWNER at agency A
 * has no privileges at agency B. The tenant scoping is enforced by the
 * fact that user lookups always happen inside the current tenant's schema.
 */
public enum UserRole {

    /**
     * The agency owner. Created during signup, cannot be deleted by
     * other users in the same agency. Has all permissions.
     */
    OWNER,

    /**
     * Administrative role. Can manage users, clients, and billing,
     * but cannot delete the agency itself.
     */
    ADMIN,

    /**
     * Standard team member. Can manage clients and run agent tasks
     * but cannot manage users or billing.
     */
    MEMBER,

    /**
     * Read-only access. Useful for stakeholders, auditors, or trial users.
     */
    VIEWER
}
