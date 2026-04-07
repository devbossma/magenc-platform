package com.magenc.platform.tenancy;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provisions new tenant schemas and runs the tenant migration set against them.
 *
 * <p>For STARTER and PRO agencies: creates a fresh schema in the shared
 * cluster. For ENTERPRISE agencies: this same lifecycle will run against
 * a separate physical database (deferred until first enterprise customer).
 */
@Service
public class TenantProvisioningService {

    private final DataSource dataSource;

    public TenantProvisioningService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Transactional
    public void provisionSchema(String tenantSlug) {
        if (!tenantSlug.matches("^[a-z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid tenant slug: " + tenantSlug);
        }
        String schemaName = TenantConnectionProvider.SCHEMA_PREFIX + tenantSlug;

        try (Connection conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        } catch (SQLException e) {
            throw new TenantProvisioningException(
                    "Failed to create schema " + schemaName, e);
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }

    public void migrateExistingSchema(String tenantSlug) {
        String schemaName = TenantConnectionProvider.SCHEMA_PREFIX + tenantSlug;
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations("classpath:db/migration/tenant")
                .load()
                .migrate();
    }

    public static class TenantProvisioningException extends RuntimeException {
        public TenantProvisioningException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
