package com.nhnacademy.insightonauth.client.impl;

import com.nhnacademy.insightonauth.dto.oauth.OauthUserInfo;
import com.nhnacademy.insightonauth.exception.email.EmailNotFoundException;
import com.nhnacademy.insightonauth.exception.external.OauthProviderResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class GithubOauthClientTest {

    private RestClient restClient;
    private RestClient.RequestHeadersUriSpec getUriSpec;
    private GithubOauthClient githubOauthClient;

    @BeforeEach
    void setUp() {
        githubOauthClient = new GithubOauthClient();
        ReflectionTestUtils.setField(githubOauthClient, "clientId", "github-client-id");
        ReflectionTestUtils.setField(githubOauthClient, "clientSecret", "github-client-secret");
        ReflectionTestUtils.setField(githubOauthClient, "redirectUri", "https://auth.insighton.store/oauth/callback");

        restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        ReflectionTestUtils.setField(githubOauthClient, "restClient", restClient);

        // access token 발급(post) — .header(String, String...)가 varargs라 deep stub 자동 체이닝이
        // 불안정해서(NPE) 직접 목을 만들어 배선한다.
        RestClient.RequestBodyUriSpec postUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(restClient.post()).thenReturn(postUriSpec);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        when(postUriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        RestClient.ResponseSpec tokenResponseSpec = mock(RestClient.ResponseSpec.class);
        when(bodySpec.retrieve()).thenReturn(tokenResponseSpec);
        when(tokenResponseSpec.body(Map.class)).thenReturn(Map.of("access_token", "github-access-token"));

        // get 경로는 /user, /user/emails 두 군데를 서로 다른 응답으로 나눠야 해서 uri별로 직접 배선
        getUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(getUriSpec);
    }

    private void stubGet(String uri, Class bodyType, Object body) {
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        when(getUriSpec.uri(uri)).thenReturn(headersSpec);
        when(headersSpec.header(eq("Authorization"), anyString())).thenReturn(headersSpec);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(bodyType)).thenReturn(body);
    }

    @Test
    @DisplayName("getUserInfo - name이 있으면 name을, primary+verified 이메일을 사용한다")
    void getUserInfo_withName() {
        stubGet("https://api.github.com/user", Map.class,
                Map.of("name", "Octo Cat", "login", "octocat", "id", 12345));
        stubGet("https://api.github.com/user/emails", List.class, List.of(
                Map.of("email", "secondary@github.com", "primary", false, "verified", true),
                Map.of("email", "primary@github.com", "primary", true, "verified", true)));

        OauthUserInfo userInfo = githubOauthClient.getUserInfo("auth-code");

        assertThat(userInfo.email()).isEqualTo("primary@github.com");
        assertThat(userInfo.name()).isEqualTo("Octo Cat");
        assertThat(userInfo.providerId()).isEqualTo("12345");
    }

    @Test
    @DisplayName("getUserInfo - name이 없으면 login(아이디)으로 대체")
    void getUserInfo_noName_fallsBackToLogin() {
        stubGet("https://api.github.com/user", Map.class,
                Map.of("login", "octocat", "id", 12345));
        stubGet("https://api.github.com/user/emails", List.class, List.of(
                Map.of("email", "primary@github.com", "primary", true, "verified", true)));

        OauthUserInfo userInfo = githubOauthClient.getUserInfo("auth-code");

        assertThat(userInfo.name()).isEqualTo("octocat");
    }

    @Test
    @DisplayName("getUserInfo - primary+verified 이메일이 없으면 예외")
    void getUserInfo_noPrimaryVerifiedEmail() {
        stubGet("https://api.github.com/user", Map.class,
                Map.of("name", "Octo Cat", "id", 12345));
        stubGet("https://api.github.com/user/emails", List.class, List.of(
                Map.of("email", "secondary@github.com", "primary", false, "verified", true)));

        assertThatThrownBy(() -> githubOauthClient.getUserInfo("auth-code"))
                .isInstanceOf(EmailNotFoundException.class);
    }

    @Test
    @DisplayName("getUserInfo - 액세스 토큰 응답이 없으면 예외")
    void getUserInfo_nullTokenResponse() {
        RestClient.RequestBodyUriSpec postUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(restClient.post()).thenReturn(postUriSpec);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        when(postUriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        RestClient.ResponseSpec tokenResponseSpec = mock(RestClient.ResponseSpec.class);
        when(bodySpec.retrieve()).thenReturn(tokenResponseSpec);
        when(tokenResponseSpec.body(Map.class)).thenReturn(null);

        assertThatThrownBy(() -> githubOauthClient.getUserInfo("auth-code"))
                .isInstanceOf(OauthProviderResponseException.class);
    }

    @Test
    @DisplayName("getUserInfo - 사용자 정보 응답이 없으면 예외")
    void getUserInfo_nullUserInfoResponse() {
        stubGet("https://api.github.com/user", Map.class, null);

        assertThatThrownBy(() -> githubOauthClient.getUserInfo("auth-code"))
                .isInstanceOf(OauthProviderResponseException.class);
    }

    @Test
    @DisplayName("getUserInfo - 이메일 목록 응답이 없으면 예외")
    void getUserInfo_nullEmailsResponse() {
        stubGet("https://api.github.com/user", Map.class,
                Map.of("name", "Octo Cat", "id", 12345));
        stubGet("https://api.github.com/user/emails", List.class, null);

        assertThatThrownBy(() -> githubOauthClient.getUserInfo("auth-code"))
                .isInstanceOf(OauthProviderResponseException.class);
    }
}
