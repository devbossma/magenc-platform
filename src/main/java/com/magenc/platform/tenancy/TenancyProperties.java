package com.magenc.platform.tenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for multi-tenancy.
 */
@ConfigurationProperties("magenc.tenancy")
public record TenancyProperties(
        String rootDomain,
        List<String> publicPaths
) {
}
