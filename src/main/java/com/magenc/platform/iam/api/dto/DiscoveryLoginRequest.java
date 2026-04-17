package com.magenc.platform.iam.api.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DiscoveryLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
