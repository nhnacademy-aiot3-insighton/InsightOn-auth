package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.auth.TokenRefreshResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.dto.auth.UserSignupResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;

import java.util.List;

public interface UserService {

    UserLoginResponse login(String email, String password);

    void logout(Long userId, String accessToken);

    void forceLogout(Long userId);

    UserLoginResponse oauthLogin(String provider, String code);

    TokenRefreshResponse refresh(Long userId, String refreshToken);
}
