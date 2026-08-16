package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;

public interface UserEmailService {

    void emailVerifyRequest(String email);
    String emailVerifyConfirm(String email, String code);

    void reactivateRequest(String email);
    UserLoginResponse reactivateConfirm(String email, String code);
    UserLoginResponse reactive(String reactiveToken);

    void passwordResetRequest(String email);
    void passwordResetConfirm(String token, String newPassword);
}
