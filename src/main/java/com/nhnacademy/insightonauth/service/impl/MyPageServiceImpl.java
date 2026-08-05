package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.client.OauthClient;
import com.nhnacademy.insightonauth.dto.mypage.MyInfoResponse;
import com.nhnacademy.insightonauth.dto.mypage.RoleResponse;
import com.nhnacademy.insightonauth.dto.oauth.OauthResponse;
import com.nhnacademy.insightonauth.dto.oauth.OauthUserInfo;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserCredential;
import com.nhnacademy.insightonauth.exception.InvalidCredentialsException;
import com.nhnacademy.insightonauth.exception.OauthAlreadyLinkedException;
import com.nhnacademy.insightonauth.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MyPageServiceImpl implements MyPageService {

    private final UserService userService;
    private final UserCredentialService userCredentialService;
    private final UserRoleService userRoleService;
    private final OauthService oauthService;
    private final OauthClient oauthClient;
    private final PasswordEncoder passwordEncoder;

    @Override
    public MyInfoResponse findMyInfo(Long userId) {
        User user = userService.findById(userId);

        return new MyInfoResponse(user.getEmail(), user.getUserName(), user.getPhoneNumber(), user.getCreatedAt());
    }

    @Override
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = userService.findById(userId);
        UserCredential userCredential = userCredentialService.findByUser(user);

        if (!passwordEncoder.matches(currentPassword, userCredential.getPasswordHash())) {
            throw new InvalidCredentialsException("기존 비밀번호가 올바르지 않습니다.");
        }

        userCredentialService.updatePassword(OffsetDateTime.now(ZoneOffset.UTC), user, newPassword);
    }

    @Override
    public List<RoleResponse> findMyRoles(Long userId) {
        User user = userService.findById(userId);

        return userRoleService.findByUser(user).stream()
                .map(userRole -> new RoleResponse(userRole.getRole()))
                .toList();
    }

    @Override
    public List<OauthResponse> findMyOauths(Long userId) {
        User user = userService.findById(userId);

        return oauthService.findAllByUser(user).stream()
                .map(userOauth -> new OauthResponse(userOauth.getOauthId(), userOauth.getProvider()))
                .toList();
    }

    @Override
    public void linkOauth(Long userId, String provider, String code) {
        User user = userService.findById(userId);   // 이미 로그인된 그 사람

        OauthUserInfo userInfo = oauthClient.getUserInfo(provider, code);   // Google 검증

        // 이 소셜 계정이 이미 다른 사람 것인지 확인 (중요!)
        oauthService.findByProviderAndProviderUserId(provider, userInfo.providerId())
                .ifPresent(existing -> {
                    throw new OauthAlreadyLinkedException("이미 연동된 소셜 계정입니다.");
                });

        oauthService.create(user, provider, userInfo.providerId());
    }
}
