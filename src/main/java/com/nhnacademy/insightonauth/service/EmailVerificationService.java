package com.nhnacademy.insightonauth.service;

public interface EmailVerificationService {

    void sendVerificationCode(String email);

    void sendReactiveVerificationCode(String email);

    void sendPasswordResetPath(String email);

    String emailCodeVerify(String email, String inputCode);

    boolean emailVerifyCheck(String email, String inputToken);

    String emailTokenVerify(String token);

    void emailReactiveVerifyCheck(String email, String inputCode);
}
