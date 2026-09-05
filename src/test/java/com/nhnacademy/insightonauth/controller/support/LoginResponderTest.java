package com.nhnacademy.insightonauth.controller.support;

import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LoginResponderTest {

    private LoginResponder loginResponder;

    @BeforeEach
    void setUp() {
        loginResponder = new LoginResponder(Duration.ofMinutes(15), Duration.ofDays(15), true);
    }

    @Test
    @DisplayName("success - access는 바디, refresh는 Set-Cookie 헤더로")
    void success() {
        UserLoginResult result = UserLoginResult.success("access-token", "refresh-token");

        ResponseEntity<UserLoginResponse> response = loginResponder.success(result);

        assertThat(response.getBody().accessToken()).isEqualTo("access-token");
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("refreshToken=refresh-token");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
    }

    @Test
    @DisplayName("accessTokenCookie - access 토큰 유효기간으로 쿠키 생성")
    void accessTokenCookie() {
        ResponseCookie cookie = loginResponder.accessTokenCookie("access-token");

        assertThat(cookie.getName()).isEqualTo("accessToken");
        assertThat(cookie.getValue()).isEqualTo("access-token");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(15));
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
    }

    @Test
    @DisplayName("refreshTokenCookie - refresh 토큰 유효기간으로 쿠키 생성")
    void refreshTokenCookie() {
        ResponseCookie cookie = loginResponder.refreshTokenCookie("refresh-token");

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(15));
    }

    @Test
    @DisplayName("cookieSecure=false면 쿠키에 Secure 속성이 빠짐")
    void cookieNotSecure() {
        LoginResponder devResponder = new LoginResponder(Duration.ofMinutes(15), Duration.ofDays(15), false);

        ResponseCookie cookie = devResponder.accessTokenCookie("access-token");

        assertThat(cookie.isSecure()).isFalse();
    }
}
