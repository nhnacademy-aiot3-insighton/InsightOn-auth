package com.nhnacademy.insightonauth.dto;

public record UserLoginResponse(
        String accessToken,
        String refreshToken
) {
}
