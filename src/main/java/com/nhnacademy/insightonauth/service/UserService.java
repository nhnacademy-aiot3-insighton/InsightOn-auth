package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.auth.TokenRefreshResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.dto.auth.UserSignupResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;

import java.util.List;

public interface UserService {

    void emailVerifyRequest(String email);

    String emailVerifyConfirm(String email, String code);

    UserLoginResponse login(String email, String password);

    void logout(Long userId, String accessToken);

    void forceLogout(Long userId);

    void reactivateRequest(String email);

    UserLoginResponse reactivateConfirm(String email, String code);

    UserLoginResponse reactive(String restoreToken);

    void passwordResetRequest(String email);

    void passwordResetConfirm(String token, String newPassword);

    UserLoginResponse oauthLogin(String provider, String code);

    TokenRefreshResponse refresh(Long userId, String refreshToken);
}
