package com.magenc.platform.iam.api.dto;

public record MeResponse(
        String userId,
        String email,
        String tenant,
        String role
) {}
