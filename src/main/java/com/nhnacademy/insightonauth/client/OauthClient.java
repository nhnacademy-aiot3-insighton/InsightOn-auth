package com.nhnacademy.insightonauth.client;

import com.nhnacademy.insightonauth.dto.oauth.OauthUserInfo;

public interface OauthClient {

    OauthUserInfo getUserInfo(String code);
}
