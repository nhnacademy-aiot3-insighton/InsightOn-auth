package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.dto.auth.UserSignupResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;

import java.util.List;

public interface UserManagementService {
    UserSignupResponse createUser(String email, String password, String userName,
                                  String phoneNumber, Role role, String verificationToken);

    boolean checkEmailAvailable(String email);

    User findById(Long userId);

    User findByEmail(String email);

    void activate(Long userId);

    void withdraw(Long userId, String accessToken);

    void sleep(Long userId);

    void block(Long userId);

    void deleteUser(Long userId);

    void updateUserName(Long userId, String newUserName);

    void updatePhoneNumber(Long userId, String phoneNumber);

    String findMaskedEmail(String userName, String phoneNumber);

    List<User> findExpiredWithdrawnUsers();

    List<User> findInactiveUsers();
}
