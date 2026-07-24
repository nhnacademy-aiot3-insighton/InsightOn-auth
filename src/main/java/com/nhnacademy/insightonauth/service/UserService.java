package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.entity.User;

public interface UserService {

    void createUser(String email, String password, String userName, String phoneNumber);

    User findById(Long userId);

    User findByEmail(String email);

    boolean login(String email, String password);

    void updateUserName(Long userId, String newUserName);

    void updatePhoneNumber(Long userId, String phoneNumber);

    void updateLastLoginAt(Long userId);

    void activate(Long userId);

    void withdraw(Long userId);

    void sleep(Long userId);

    void block(Long userId);

    void deleteUser(Long userId);
}
