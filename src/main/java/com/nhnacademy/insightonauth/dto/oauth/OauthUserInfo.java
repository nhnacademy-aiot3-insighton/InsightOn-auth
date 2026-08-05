package com.nhnacademy.insightonauth.dto.oauth;

public record OauthUserInfo(
        String email,
        String name,
        String providerId
) {

}