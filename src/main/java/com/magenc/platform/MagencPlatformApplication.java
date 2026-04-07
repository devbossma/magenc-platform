package com.magenc.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;

/**
 * Magenc Platform - Marketing Agency Orchestration SaaS.
 *
 * <p>Modular monolith. Module boundaries are enforced at build time by
 * Spring Modulith. Each top-level package directly under
 * {@code com.magenc.platform} is a module.
 */
@SpringBootApplication
@Modulithic(systemName = "Magenc Platform")
@ConfigurationPropertiesScan
public class MagencPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MagencPlatformApplication.class, args);
    }
}
