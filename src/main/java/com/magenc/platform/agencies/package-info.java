/**
 * Agencies module.
 *
 * <p>Owns the agency catalog, memberships, and the signup orchestration flow.
 * Depends on {@code iam} for user identity and {@code tenancy} for schema provisioning.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Agencies",
    allowedDependencies = { "iam", "tenancy" }
)
package com.magenc.platform.agencies;
