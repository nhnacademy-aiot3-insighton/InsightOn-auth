package com.nhnacademy.insightonauth.dto.auth;

public record TokenRefreshResponse(
        String accessToken,
        String tokenType
) {
    public TokenRefreshResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}