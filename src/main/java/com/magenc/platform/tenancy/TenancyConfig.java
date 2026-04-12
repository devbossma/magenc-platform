package com.magenc.platform.tenancy;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Plugs the multi-tenancy components into Hibernate's configuration
 * via {@link HibernatePropertiesCustomizer}.
 */
@Configuration
@EnableConfigurationProperties(TenancyProperties.class)
public class TenancyConfig implements HibernatePropertiesCustomizer {

    private final TenantConnectionProvider connectionProvider;
    private final TenantSchemaResolver schemaResolver;

    public TenancyConfig(
            TenantConnectionProvider connectionProvider,
            TenantSchemaResolver schemaResolver) {
        this.connectionProvider = connectionProvider;
        this.schemaResolver = schemaResolver;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, schemaResolver);
    }
}
