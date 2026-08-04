package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.entity.Oauth;
import com.nhnacademy.insightonauth.entity.User;

import java.util.List;

public interface OauthService {

    void create(User user, String provider, String providerUserId);

    void delete(User user, Long oauthId);

    void deleteAllByUser(User user);

    Oauth findOauth(User user, String provider);

    List<Oauth> findAllByUser(User user);
}
