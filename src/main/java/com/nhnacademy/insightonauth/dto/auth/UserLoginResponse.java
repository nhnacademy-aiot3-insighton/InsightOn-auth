package com.nhnacademy.insightonauth.dto.auth;

/**
 * 로그인/관리자 로그인 HTTP 응답 바디.
 * <ul>
 *   <li>{@code SUCCESS} — accessToken 유효, restoreToken 은 null. refresh 토큰은 HttpOnly 쿠키로 별도 전달된다.</li>
 *   <li>{@code PENDING_RESTORE} — 탈퇴 후 복구 가능 기간 내 계정. 로그인 성공이 아니며
 *       accessToken 은 null, restoreToken 만 채워진다. 클라이언트는 이 토큰으로 복구 화면으로 진입한다.</li>
 * </ul>
 */
public record UserLoginResponse(
        String status,
        String accessToken,
        String restoreToken
) {

    public static UserLoginResponse success(String accessToken) {
        return new UserLoginResponse("SUCCESS", accessToken, null);
    }

    public static UserLoginResponse pendingRestore(String restoreToken) {
        return new UserLoginResponse("PENDING_RESTORE", null, restoreToken);
    }
}
