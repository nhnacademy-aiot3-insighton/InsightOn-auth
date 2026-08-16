package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.auth.TokenRefreshResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;

public interface UserAuthenticationService {

    UserLoginResponse login(String email, String password);

    void logout(Long userId, String accessToken);

    void forceLogout(Long userId);

    UserLoginResponse oauthLogin(String provider, String code);

    TokenRefreshResponse refresh(Long userId, String refreshToken);
}
