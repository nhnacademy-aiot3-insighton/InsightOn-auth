package com.nhnacademy.insightonauth.dto;

public record UserLoginResponse(
        String status,
        String accessToken,
        String tokenType,
        String refreshToken,
        String restoreToken
) {
    public static UserLoginResponse success(String accessToken, String refreshToken) {
        return new UserLoginResponse("SUCCESS", accessToken, "Bearer", refreshToken, null);
    }

    public static UserLoginResponse pendingRestore(String restoreToken) {
        return new UserLoginResponse("PENDING_RESTORE", null, null, null, restoreToken);
    }
}
