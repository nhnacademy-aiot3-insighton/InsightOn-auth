package com.nhnacademy.insightonauth.dto.auth;

/**
 * 인증 서비스 계층의 로그인 처리 결과(내부 반환값).
 * refreshToken 을 포함하며, 컨트롤러가 이를 HttpOnly 쿠키로 옮기고
 * HTTP 응답 바디로는 {@link UserLoginResponse} 로 변환한다.
 */
public record UserLoginResult(
        String status,
        String accessToken,
        String tokenType,
        String refreshToken
) {
    public static UserLoginResult success(String accessToken, String refreshToken) {
        return new UserLoginResult("SUCCESS", accessToken, "Bearer", refreshToken);
    }

    public static UserLoginResult pendingRestore() {
        return new UserLoginResult("PENDING_RESTORE", null, null, null);
    }
}
