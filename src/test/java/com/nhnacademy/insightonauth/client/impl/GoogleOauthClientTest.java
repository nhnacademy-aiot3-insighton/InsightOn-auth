package com.nhnacademy.insightonauth.client.impl;

import com.nhnacademy.insightonauth.dto.oauth.OauthUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class GoogleOauthClientTest {

    private RestClient restClient;
    private GoogleOauthClient googleOauthClient;

    @BeforeEach
    void setUp() {
        googleOauthClient = new GoogleOauthClient();
        ReflectionTestUtils.setField(googleOauthClient, "clientId", "google-client-id");
        ReflectionTestUtils.setField(googleOauthClient, "clientSecret", "google-client-secret");
        ReflectionTestUtils.setField(googleOauthClient, "redirectUri", "https://auth.insighton.store/oauth/callback");

        restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        ReflectionTestUtils.setField(googleOauthClient, "restClient", restClient);

        // 토큰 교환(post)은 .header(...)가 없어 deep stub 체인이 그대로 먹힌다.
        when(restClient.post().uri(anyString()).contentType(any()).body(any(Object.class)).retrieve().body(Map.class))
                .thenReturn(Map.of("access_token", "google-access-token"));

        // 유저정보 조회(get)는 .header(String, String...)가 varargs라 deep stub 자동 체이닝이
        // 불안정해서(NPE) 직접 목을 만들어 배선한다.
        RestClient.RequestHeadersUriSpec getUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(getUriSpec);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        when(getUriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class))
                .thenReturn(Map.of("email", "test@gmail.com", "name", "Test User", "sub", "google-sub-id"));
    }

    @Test
    @DisplayName("getUserInfo - 코드로 액세스 토큰을 받아 사용자 정보를 조회한다")
    void getUserInfo_success() {
        OauthUserInfo userInfo = googleOauthClient.getUserInfo("auth-code");

        assertThat(userInfo.email()).isEqualTo("test@gmail.com");
        assertThat(userInfo.name()).isEqualTo("Test User");
        assertThat(userInfo.providerId()).isEqualTo("google-sub-id");
    }
}
