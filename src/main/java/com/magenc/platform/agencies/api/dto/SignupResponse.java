package com.magenc.platform.agencies.api.dto;

public record SignupResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        String tokenType,
        String agencySlug,
        String agencyDisplayName,
        String userId
) {}
