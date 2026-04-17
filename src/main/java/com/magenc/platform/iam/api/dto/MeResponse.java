package com.magenc.platform.iam.api.dto;

public record MeResponse(String userId, String email, String displayName, String tenant, String agencyId, String role, String sessionId) {}
