package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserCredential;

public interface UserCredentialService {
    void create(User user, String password);

    UserCredential findByUser(User user);

    void updatePassword(User user, String password);
}
