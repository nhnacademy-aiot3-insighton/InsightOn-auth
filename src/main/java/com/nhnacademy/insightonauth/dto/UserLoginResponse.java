package com.nhnacademy.insightonauth.dto;

public record UserLoginResponse(
        String accessToken,
        String tokenType,
        String refreshToken
) {
    public UserLoginResponse(String accessToken, String refreshToken) {
        this(accessToken, "Bearer", refreshToken);
    }
}
