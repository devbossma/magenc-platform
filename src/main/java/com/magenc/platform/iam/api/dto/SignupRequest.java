package com.magenc.platform.iam.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank
        @Pattern(regexp = "^[a-z0-9_]{3,63}$",
                 message = "tenantSlug must be 3-63 lowercase alphanumeric characters or underscores")
        String tenantSlug,

        @NotBlank
        @Size(max = 255)
        String agencyDisplayName,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 12, max = 128)
        String password,

        @NotBlank
        @Size(max = 255)
        String displayName
) {}
