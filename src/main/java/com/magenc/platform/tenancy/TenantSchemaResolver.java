package com.magenc.platform.tenancy;

import jakarta.validation.constraints.NotNull;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Tells Hibernate which tenant to use for the current operation.
 *
 * <p>The fallback "admin" tenant is used during application startup,
 * Flyway migration, and any background job that operates on platform
 * metadata rather than a specific agency's data.
 */
@Component
public class TenantSchemaResolver implements CurrentTenantIdentifierResolver {

    public static final String DEFAULT_TENANT = "admin";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.get();
        return tenant != null ? tenant : DEFAULT_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    public boolean isRoot(String tenantIdentifier) {
        return DEFAULT_TENANT.equals(tenantIdentifier);
    }
}
