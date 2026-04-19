package com.magenc.platform.tenancy;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Ensures Flyway migrations run correctly with multi-tenancy configuration.
 *
 * Flyway needs to run migrations on the "admin" schema before Hibernate JPA
 * tries to use the tables. This configuration explicitly manages Flyway initialization.
 */
@Configuration
public class FlywayConfig {

    /**
     * Ensure Flyway bean is created early so migrations run before JPA initialization.
     * This is critical for multi-tenancy where the admin schema must exist
     * before Hibernate tries to access it.
     */
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/admin")
                .schemas("admin")
                .defaultSchema("admin")
                .baselineOnMigrate(true)
                .load();
    }
}

