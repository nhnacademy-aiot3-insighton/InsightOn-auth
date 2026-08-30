package com.nhnacademy.insightonauth.service;

import com.nhnacademy.insightonauth.entity.Oauth;
import com.nhnacademy.insightonauth.entity.User;

import java.util.List;
import java.util.Optional;

public interface OauthService {

    void create(User user, String provider, String providerUserId);

    void delete(User user, Long oauthId);

    void deleteAllByUser(User user);

    Oauth findOauth(User user, String provider);

    List<Oauth> findAllByUser(User user);

    Optional<Oauth> findByProviderAndProviderUserId(String provider, String providerUserId);

    /** 이 유저가 해당 provider 소셜 계정을 이미 연동했는지 여부 (계정당 provider 1개 제한용). */
    boolean hasProviderLinked(User user, String provider);

    /** 유저의 모든 연동 행에 탈퇴 마스킹 접미사를 붙인다. */
    void maskByUser(User user);

    /** 유저의 마스킹된 연동 행을 원복한다. 원본 식별자가 이미 사용 중이면 예외. */
    void reactivateByUser(User user);

    /** 마스킹된(탈퇴) 연동을 원본 provider_user_id 로 조회한다. */
    Optional<Oauth> findReactivatableByProviderAndProviderUserId(String provider, String providerUserId);
}
