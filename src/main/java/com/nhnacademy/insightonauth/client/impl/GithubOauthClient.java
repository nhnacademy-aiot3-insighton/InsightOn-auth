package com.nhnacademy.insightonauth.client.impl;

import com.nhnacademy.insightonauth.client.OauthClient;
import com.nhnacademy.insightonauth.dto.oauth.OauthUserInfo;
import com.nhnacademy.insightonauth.exception.EmailNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component("githubOauthClient")
@RequiredArgsConstructor
public class GithubOauthClient implements OauthClient {

    @Value("${oauth.github.client-id}")
    private String clientId;

    @Value("${oauth.github.client-secret}")
    private String clientSecret;

    @Value("${oauth.redirect-uri}")
    private String redirectUri;

    private final RestClient restClient = RestClient.create();

    @Override
    public OauthUserInfo getUserInfo(String code) {
        String accessToken = requestAccessToken(code);
        return requestUserInfo(accessToken);
    }

    private String requestAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);

        Map<String, Object> response = restClient.post()
                .uri("https://github.com/login/oauth/access_token")   // GitHub URL로 수정
                .header("Accept", "application/json")                  // JSON 응답 요청 (필수)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(Map.class);

        return (String) response.get("access_token");
    }

    private OauthUserInfo requestUserInfo(String accessToken) {
        Map<String, Object> userInfo = restClient.get()
                .uri("https://api.github.com/user")   // GitHub URL로 수정
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        String email = (String) userInfo.get("email");
        if (email == null) {
            email = requestPrimaryEmail(accessToken);   // 비공개 이메일 대응
        }

        String name = (String) userInfo.get("name");
        if (name == null || name.isBlank()) {
            name = (String) userInfo.get("login");   // name이 없으면 login(아이디)으로 대체
        }

        Object id = userInfo.get("id");   // GitHub은 sub가 아니라 id (숫자)

        return new OauthUserInfo(
                email,
                name,
                String.valueOf(id)
        );
    }

    // 이메일 열람 불가의 경우 api를 통해서 가져와야함
    private String requestPrimaryEmail(String accessToken) {
        List<Map<String, Object>> emails = restClient.get()
                .uri("https://api.github.com/user/emails")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(List.class);

        return emails.stream()
                .filter(e -> Boolean.TRUE.equals(e.get("primary")))
                .map(e -> (String) e.get("email"))
                .findFirst()
                .orElseThrow(() -> new EmailNotFoundException("GitHub 계정에서 이메일을 찾을 수 없습니다."));
    }
}
