package com.nhnacademy.insightonauth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 브라우저 주도 소셜 로그인({@code GET /oauth/authorize/{provider}} → provider → {@code GET /oauth/callback})
 * 에 필요한 값과 조립 로직. client-id/redirect-uri/프론트 주소는 환경별로 다르므로 주입받고,
 * provider 별 authorize 엔드포인트·scope 는 사실상 상수라 여기 고정한다.
 */
@Component
public class OauthWebSupport {

    /** CSRF 방지 state 쿠키. 값 형식 {@code <nonce>.<provider>}. */
    public static final String STATE_COOKIE = "oauthState";
    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    private final String frontUrl;
    private final String redirectUri;
    /** dev(http)에서는 false 로 내려야 브라우저가 state 쿠키를 콜백에 실어 보낸다. prod(https)는 true. */
    private final boolean cookieSecure;
    private final Map<String, Provider> providers;

    OauthWebSupport(
            @Value("${app.front-url}") String frontUrl,
            @Value("${oauth.redirect-uri}") String redirectUri,
            @Value("${app.cookie.secure:true}") boolean cookieSecure,
            @Value("${oauth.google.client-id}") String googleClientId,
            @Value("${oauth.github.client-id}") String githubClientId) {
        this.frontUrl = stripTrailingSlash(frontUrl);
        this.redirectUri = redirectUri;
        this.cookieSecure = cookieSecure;
        this.providers = Map.of(
                "google", new Provider(googleClientId,
                        "https://accounts.google.com/o/oauth2/v2/auth", "openid email profile"),
                "github", new Provider(githubClientId,
                        "https://github.com/login/oauth/authorize", "read:user user:email"));
    }

    public boolean supports(String provider) {
        return providers.containsKey(provider);
    }

    /** provider 동의 화면 URL. */
    public String authorizeUrl(String provider, String state) {
        Provider p = providers.get(provider);
        return UriComponentsBuilder.fromUriString(p.authorizationUri())
                .queryParam("client_id", p.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", p.scope())
                .queryParam("state", state)
                .encode(StandardCharsets.UTF_8)
                .build().toUriString();
    }

    public ResponseCookie stateCookie(String state) {
        return baseStateCookie(state).maxAge(STATE_TTL).build();
    }

    public ResponseCookie expiredStateCookie() {
        return baseStateCookie("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseStateCookie(String value) {
        return ResponseCookie.from(STATE_COOKIE, value)
                .httpOnly(true).secure(cookieSecure).path("/").sameSite("Lax");
    }

    /** 프론트 절대 URL (예: {@code https://insighton.store/login?oauthError=1}). */
    public String front(String path) {
        return frontUrl + path;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record Provider(String clientId, String authorizationUri, String scope) {
    }
}
