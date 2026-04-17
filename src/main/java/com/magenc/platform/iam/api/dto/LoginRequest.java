package com.magenc.platform.iam.api.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9_]{3,63}$") String tenantSlug,
        @NotBlank @Email String email,
        @NotBlank String password
) {}
