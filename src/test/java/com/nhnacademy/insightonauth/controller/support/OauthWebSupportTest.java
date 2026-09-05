package com.nhnacademy.insightonauth.controller.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

class OauthWebSupportTest {

    private OauthWebSupport oauthWebSupport;

    @BeforeEach
    void setUp() {
        oauthWebSupport = new OauthWebSupport(
                "https://insighton.store/",
                "https://auth.insighton.store/oauth/callback",
                true,
                "google-client-id",
                "github-client-id");
    }

    @Test
    @DisplayName("등록된 provider만 지원")
    void supports() {
        assertThat(oauthWebSupport.supports("google")).isTrue();
        assertThat(oauthWebSupport.supports("github")).isTrue();
        assertThat(oauthWebSupport.supports("kakao")).isFalse();
    }

    @Test
    @DisplayName("authorizeUrl - client_id/redirect_uri/state 등이 쿼리로 포함됨")
    void authorizeUrl_google() {
        String url = oauthWebSupport.authorizeUrl("google", "nonce.google");

        assertThat(url)
                .startsWith("https://accounts.google.com/o/oauth2/v2/auth")
                .contains("client_id=google-client-id")
                .contains("state=nonce.google")
                .contains("response_type=code");
    }

    @Test
    @DisplayName("authorizeUrl - github은 github 동의 화면으로")
    void authorizeUrl_github() {
        String url = oauthWebSupport.authorizeUrl("github", "nonce.github");

        assertThat(url)
                .startsWith("https://github.com/login/oauth/authorize")
                .contains("client_id=github-client-id");
    }

    @Test
    @DisplayName("stateCookie - httpOnly, secure, 5분 TTL")
    void stateCookie() {
        ResponseCookie cookie = oauthWebSupport.stateCookie("nonce.google");

        assertThat(cookie.getName()).isEqualTo("oauthState");
        assertThat(cookie.getValue()).isEqualTo("nonce.google");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getMaxAge().toMinutes()).isEqualTo(5);
    }

    @Test
    @DisplayName("expiredStateCookie - 값 비우고 즉시 만료")
    void expiredStateCookie() {
        ResponseCookie cookie = oauthWebSupport.expiredStateCookie();

        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().isZero()).isTrue();
    }

    @Test
    @DisplayName("front - 프론트 URL 뒤에 경로를 붙임 (끝 슬래시는 제거된 상태)")
    void front() {
        assertThat(oauthWebSupport.front("/login?oauthError=1"))
                .isEqualTo("https://insighton.store/login?oauthError=1");
    }
}
