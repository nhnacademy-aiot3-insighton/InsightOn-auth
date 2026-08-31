package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.auth.UserLoginResult;

public interface UserEmailService {

    void emailVerifyRequest(String email);
    String emailVerifyConfirm(String email, String code);

    void reactivateRequest(String email);
    UserLoginResult reactivateConfirm(String email, String code);
    UserLoginResult reactive(String reactiveToken);

    void passwordResetRequest(String email);
    void passwordResetConfirm(String token, String newPassword);
}
