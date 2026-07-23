package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.entity.User;

import java.util.UUID;

public interface UserService {

    void createUser(String email, String userName, String phoneNumber);

    User findById(UUID userId);

    User findByEmail(String email);

    void updateUserName(UUID userId, String newUserName);

    void updatePhoneNumber(UUID userId, String phoneNumber);

    void updateLastLoginAt(UUID userId);

    void activate(UUID userId);

    void withdraw(UUID userId);

    void sleep(UUID userId);

    void block(UUID userId);
}
