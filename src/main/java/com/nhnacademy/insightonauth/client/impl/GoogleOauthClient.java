package com.nhnacademy.insightonauth.client.impl;

import com.nhnacademy.insightonauth.client.OauthClient;
import com.nhnacademy.insightonauth.dto.oauth.OauthUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component("googleOauthClient")
@RequiredArgsConstructor
public class GoogleOauthClient implements OauthClient {

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
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
//        FormHttpMessageConverter
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        Map<String, Object> response = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(Map.class);

        return (String) response.get("access_token");
    }

    private OauthUserInfo requestUserInfo(String accessToken) {
        Map<String, Object> userInfo = restClient.get()
                .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        return new OauthUserInfo(
                (String) userInfo.get("email"),
                (String) userInfo.get("name"),
                (String) userInfo.get("sub")
        );
    }
}
