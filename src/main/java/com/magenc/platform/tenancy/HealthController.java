package com.magenc.platform.tenancy;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health and tenant-resolution probe endpoints.
 *
 * <p>{@code /v1/health} works without a tenant subdomain.
 * {@code /v1/health/tenant} requires a tenant subdomain - proves the
 * resolution filter is wired and the schema would be switched.
 */
@RestController
@RequestMapping("/v1/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "magenc-platform",
                "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/tenant")
    public Map<String, Object> tenantHealth() {
        return Map.of(
                "status", "ok",
                "tenant", TenantContext.require(),
                "timestamp", Instant.now().toString()
        );
    }
}
