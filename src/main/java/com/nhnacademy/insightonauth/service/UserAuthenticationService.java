package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.auth.TokenRefreshResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResult;

public interface UserAuthenticationService {

    UserLoginResult login(String email, String password);

    void logout(Long userId, String accessToken);

    void forceLogout(Long userId);

    UserLoginResult oauthLogin(String provider, String code);

    TokenRefreshResponse refresh(Long userId, String refreshToken);
}
