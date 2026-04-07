package com.magenc.platform.tenancy;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides JDBC connections scoped to a specific tenant's schema.
 *
 * <p>For each request: borrow a connection from the shared pool,
 * issue {@code SET search_path TO tenant_acme, public}, and on release
 * reset the search_path so the connection is clean when returned.
 *
 * <p>Tenant schema names are namespaced as {@code tenant_<id>} to prevent
 * collisions with reserved Postgres schemas.
 */
@Component
public class TenantConnectionProvider implements MultiTenantConnectionProvider<Object> {

    public static final String SCHEMA_PREFIX = "tenant_";
    public static final String ADMIN_SCHEMA = "admin";

    private final DataSource dataSource;

    public TenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        String tenantId = (String) tenantIdentifier;
        Connection connection = getAnyConnection();
        String schema = resolveSchema(tenantId);
        try (var stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO " + schema + ", public");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO public");
        } catch (SQLException ignored) {
            // best effort
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(@SuppressWarnings("rawtypes") Class unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new UnsupportedOperationException("Cannot unwrap to " + unwrapType);
    }

    private String resolveSchema(String tenantIdentifier) {
        if (TenantSchemaResolver.DEFAULT_TENANT.equals(tenantIdentifier)) {
            return ADMIN_SCHEMA;
        }
        // Defense in depth: only allow lowercase letters, digits, underscores
        if (!tenantIdentifier.matches("^[a-z0-9_]+$")) {
            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + tenantIdentifier);
        }
        return SCHEMA_PREFIX + tenantIdentifier;
    }
}
