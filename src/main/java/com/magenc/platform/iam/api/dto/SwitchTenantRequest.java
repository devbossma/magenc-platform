package com.magenc.platform.iam.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SwitchTenantRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9_]{3,63}$") String targetTenantSlug
) {}
