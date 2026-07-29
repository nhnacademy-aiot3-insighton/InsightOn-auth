package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.UserLoginResponse;
import com.nhnacademy.insightonauth.dto.UserSignupResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;

public interface UserService {

    UserSignupResponse createUser(String email, String password, String userName, String phoneNumber, Role role, String verificationToken);

    boolean checkEmailAvailable(String email);

    void emailVerifyRequest(String email);

    String emailVerifyConfirm(String email, String code);

    User findById(Long userId);

    User findByEmail(String email);

    UserLoginResponse login(String email, String password);

    void logout(Long userId);

    void reactivateRequest(String email);

    UserLoginResponse reactivateConfirm(String email, String code);

    void updateUserName(Long userId, String newUserName);

    void updatePhoneNumber(Long userId, String phoneNumber);

    String findMaskedEmail(String userName, String phoneNumber);

    void updateLastLoginAt(Long userId);

    void activate(Long userId);

    void withdraw(Long userId);

    void sleep(Long userId);

    void block(Long userId);

    void deleteUser(Long userId);
}
