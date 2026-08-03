package com.nhnacademy.insightonauth.dto;

public record OauthUserInfo(
        String email,
        String name,
        String providerId
) {

}