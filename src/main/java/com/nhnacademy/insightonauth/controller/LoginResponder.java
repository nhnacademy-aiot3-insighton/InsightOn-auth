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

    /** refresh 쿠키 수명 = 토큰 유효기간(jwt.refresh-token-validity)과 동일하게 맞춘다. "15d" → Spring 이 Duration 으로 변환. */
    private final Duration refreshTokenValidity;

    LoginResponder(@Value("${jwt.refresh-token-validity}") Duration refreshTokenValidity) {
        this.refreshTokenValidity = refreshTokenValidity;
    }

    ResponseEntity<UserLoginResponse> success(UserLoginResult result) {
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(true)           // https라 true
                .path("/")
                .sameSite("Lax")
                .maxAge(refreshTokenValidity)
                .build();               // domain 안 박음

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(UserLoginResponse.success(result.accessToken()));
    }
}
