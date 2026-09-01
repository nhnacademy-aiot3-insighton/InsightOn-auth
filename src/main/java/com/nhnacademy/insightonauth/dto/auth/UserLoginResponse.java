package com.nhnacademy.insightonauth.dto.auth;

/**
 * 로그인/관리자 로그인 HTTP 응답 바디.
 * <ul>
 *   <li>{@code SUCCESS} — accessToken 유효. refresh 토큰은 HttpOnly 쿠키로 별도 전달된다.</li>
 *   <li>{@code PENDING_RESTORE} — 탈퇴 후 복구 가능 기간 내 계정. 로그인 성공이 아니며 accessToken 은 null.
 *       클라이언트는 재활성화(이메일 인증 코드) 화면으로 진입한다.</li>
 * </ul>
 */
public record UserLoginResponse(
        String status,
        String accessToken
) {

    public static UserLoginResponse success(String accessToken) {
        return new UserLoginResponse("SUCCESS", accessToken);
    }

    public static UserLoginResponse pendingRestore() {
        return new UserLoginResponse("PENDING_RESTORE", null);
    }
}
