package com.magenc.platform.tenancy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configures multi-tenancy for Hibernate in Spring Boot 4.0+.
 *
 * The TenantConnectionProvider and TenantSchemaResolver are automatically
 * discovered by Hibernate through the standard Spring bean discovery mechanism.
 * This configuration class simply enables the TenancyProperties configuration.
 */
@Configuration
@EnableConfigurationProperties(TenancyProperties.class)
public class TenancyConfig {
}