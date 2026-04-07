package com.magenc.platform.tenancy;

import org.jspecify.annotations.Nullable;

/**
 * Holds the current tenant identifier for the duration of a request.
 *
 * <p>Set by {@link TenantResolutionFilter} early in the filter chain
 * and read by {@link TenantSchemaResolver} when Hibernate asks which
 * tenant the current operation belongs to.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static @Nullable String get() {
        return CURRENT_TENANT.get();
    }

    public static String require() {
        String tenant = CURRENT_TENANT.get();
        if (tenant == null) {
            throw new IllegalStateException(
                    "No tenant in context. This code path requires a resolved tenant.");
        }
        return tenant;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
