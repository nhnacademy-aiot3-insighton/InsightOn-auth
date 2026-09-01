package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 로그인 성공 응답을 만든다 — access 토큰은 바디, refresh 토큰은 HttpOnly 쿠키.
 * 일반/관리자 로그인이 이 규약을 공유하므로 한곳에 모은다.
 * (PENDING_RESTORE·역할 확인 등 분기는 컨트롤러가 담당한다.)
 */
@Component
public class LoginResponder {

    /** 쿠키 수명 = 각 토큰 유효기간(jwt.*-token-validity)과 동일하게 맞춘다. "15d"/"15m" → Spring 이 Duration 으로 변환. */
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;
    /** dev(http)에서는 false. prod(https)는 true. 기본값 true 로 prod 안전. */
    private final boolean cookieSecure;

    LoginResponder(@Value("${jwt.access-token-validity}") Duration accessTokenValidity,
                   @Value("${jwt.refresh-token-validity}") Duration refreshTokenValidity,
                   @Value("${app.cookie.secure:true}") boolean cookieSecure) {
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
        this.cookieSecure = cookieSecure;
    }

    ResponseEntity<UserLoginResponse> success(UserLoginResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.refreshToken()).toString())
                .body(UserLoginResponse.success(result.accessToken()));
    }

    /** 브라우저 주도 소셜 로그인은 바디를 못 주므로 access 토큰도 쿠키로 내려준다. */
    ResponseCookie accessTokenCookie(String accessToken) {
        return tokenCookie("accessToken", accessToken, accessTokenValidity);
    }

    ResponseCookie refreshTokenCookie(String refreshToken) {
        return tokenCookie("refreshToken", refreshToken, refreshTokenValidity);
    }

    private ResponseCookie tokenCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)   // prod https=true, dev http=false
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();               // domain 안 박음
    }
}
