package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserCredential;

import java.time.OffsetDateTime;

public interface UserCredentialService {
    void create(User user, String password);

    UserCredential findByUser(User user);

    void delete(User user);

    void updatePassword(OffsetDateTime now, User user, String password);

    boolean exists(User user);
}
