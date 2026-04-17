package com.magenc.platform.iam.api.dto;

public record TokenResponse(String accessToken, String refreshToken, long expiresInSeconds, String tokenType) {
    public static TokenResponse of(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, refreshToken, expiresInSeconds, "Bearer");
    }
}
