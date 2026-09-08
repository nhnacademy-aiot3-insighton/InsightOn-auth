package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.client.OauthClient;
import com.nhnacademy.insightonauth.client.OauthClientResolver;
import com.nhnacademy.insightonauth.dto.core.UserGroupResponse;
import com.nhnacademy.insightonauth.dto.mypage.MyInfoResponse;
import com.nhnacademy.insightonauth.dto.mypage.MyRoleResponse;
import com.nhnacademy.insightonauth.dto.oauth.OauthResponse;
import com.nhnacademy.insightonauth.dto.oauth.OauthUserInfo;
import com.nhnacademy.insightonauth.entity.Oauth;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserCredential;
import com.nhnacademy.insightonauth.exception.auth.*;
import com.nhnacademy.insightonauth.exception.user.*;
import com.nhnacademy.insightonauth.exception.oauth.*;
import com.nhnacademy.insightonauth.exception.external.*;
import com.nhnacademy.insightonauth.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class MyPageServiceImpl implements MyPageService {

    private final UserCredentialService userCredentialService;
    private final UserRoleService userRoleService;
    private final OauthService oauthService;
    private final OauthClientResolver oauthClientResolver;
    private final PasswordEncoder passwordEncoder;
    private final UserManagementService userManagementService;
    private final CoreService coreService;

    @Override
    @Transactional(readOnly = true)
    public MyInfoResponse findMyInfo(Long userId) {
        User user = userManagementService.findById(userId);
        UserGroupResponse userGroupResponse = coreService.getUserGroup(userId);

        String groupName = (userGroupResponse.exists() && userGroupResponse.groupName() != null)
                ? userGroupResponse.groupName()
                : "그룹 없음";

        return new MyInfoResponse(user.getEmail(),
                user.getUserName(),
                user.getPhoneNumber(),
                user.getCreatedAt(),
                groupName);
    }

    @Override
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = userManagementService.findById(userId);
        UserCredential userCredential = userCredentialService.findByUser(user);

        // 현재 비밀번호 확인 (새 비밀번호가 기존과 같은지는 updatePassword에서 검사)
        if (!passwordEncoder.matches(currentPassword, userCredential.getPasswordHash())) {
            throw new InvalidCredentialsException("기존 비밀번호가 올바르지 않습니다.");
        }

        userCredentialService.updatePassword(OffsetDateTime.now(ZoneOffset.UTC), user, newPassword);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyRoleResponse> findMyRoles(Long userId) {
        User user = userManagementService.findById(userId);

        return userRoleService.findByUser(user).stream()
                .map(userRole -> new MyRoleResponse(userRole.getRole()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OauthResponse> findMyOauths(Long userId) {
        User user = userManagementService.findById(userId);

        return oauthService.findAllByUser(user).stream()
                .map(userOauth -> new OauthResponse(userOauth.getOauthId(), userOauth.getProvider()))
                .toList();
    }

    @Override
    public void linkOauth(Long userId, String provider, String code) {
        User primaryUser = userManagementService.findById(userId);

        // 계정당 provider 하나만 연동 허용 — 외부 OAuth 왕복 전에 차단 (불필요한 provider 호출·code 소모 방지)
        if (oauthService.hasProviderLinked(primaryUser, provider)) {
            throw new OauthAlreadyLinkedException("이미 " + provider + " 계정이 연동되어 있습니다.");
        }

        OauthClient oauthClient = oauthClientResolver.resolve(provider);
        OauthUserInfo userInfo = oauthClient.getUserInfo(code);

        Optional<Oauth> conflictingOauth = oauthService.findByProviderAndProviderUserId(provider, userInfo.providerId());

        if (conflictingOauth.isPresent()) {
            User conflictingUser = conflictingOauth.get().getUser();

            if (conflictingUser.getUserId().equals(primaryUser.getUserId())) {
                throw new OauthAlreadyLinkedException("이미 연동된 소셜 계정입니다.");
            }

            // 다른 사람 계정에 연동되어 있음 → "병합할지" 물어봐야 하는 상황
            throw new OauthLinkedToOtherAccountException(
                    "이 계정은 이미 다른 계정에 연동되어 있습니다. 병합하시려면 확인 후 다시 요청해주세요.",
                    conflictingUser.getUserId());
        }

        oauthService.create(primaryUser, provider, userInfo.providerId());
    }

    // 다른 계정 삭제하고 하나로 합치기
    @Override
    public void mergeAccount(Long primaryUserId, Long secondaryUserId, String provider, String providerUserId) {
        // ★ 같은 계정끼리 병합 거부 (자기 자신 병합 시 계정 삭제 방지)
        if (primaryUserId.equals(secondaryUserId)) {
            throw new InvalidMergeRequestException("자기 자신과는 병합할 수 없습니다.");
        }

        User primaryUser = userManagementService.findById(primaryUserId);

        // confirmMerge는 linkOauth의 사전 검사를 거치지 않고 바로 여기로 오므로, primary가 이미
        // 같은 provider를 갖고 있으면 재연결 시 DB 유니크 제약 위반(정제 안 된 예외)이 아니라
        // 여기서 명확히 막는다.
        if (oauthService.hasProviderLinked(primaryUser, provider)) {
            throw new OauthAlreadyLinkedException("이미 " + provider + " 계정이 연동되어 있습니다.");
        }

        // 2차 확인 - secondaryUser가 정말 이 provider/providerUserId를 갖고 있는지 검증
        Oauth secondaryOauth = oauthService.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseThrow(() -> new OauthNotFoundException("연동 정보를 찾을 수 없습니다."));

        if (!secondaryOauth.getUser().getUserId().equals(secondaryUserId)) {
            throw new InvalidMergeRequestException("병합 요청이 유효하지 않습니다.");
        }

        // secondaryUser 삭제 직전 재확인 — 그룹 관리자면 병합(=계정 삭제)로 탈퇴 제한을 우회할 수 없다
        boolean isGroupManager;
        try {
            isGroupManager = coreService.isGroupManager(secondaryUserId);
        } catch (Exception e) {
            throw new CoreServiceUnavailableException(
                    "일시적으로 그룹 정보를 확인할 수 없어 병합이 제한됩니다. 잠시 후 다시 시도해주세요.");
        }

        if (isGroupManager) {
            throw new ManagerGroupExistsException("그룹 관리자 역할이 있는 계정은 병합할 수 없습니다.");
        }

        // Oauth를 primaryUser로 재연결
        secondaryOauth.reassignUser(primaryUser);

        // secondaryUser(연동 전에 사용하던 계정 삭제) 삭제
        userManagementService.deleteUser(secondaryUserId);
    }

    // 병합 확인 시 provider 재인증 왕복으로 받은 code — 이 code로 방금 교환한 providerUserId만 신뢰한다
    // (클라이언트가 주장하는 providerUserId를 그대로 믿지 않기 위함)
    @Override
    public void confirmMerge(Long primaryUserId, Long secondaryUserId, String provider, String code) {
        OauthClient oauthClient = oauthClientResolver.resolve(provider);
        OauthUserInfo userInfo = oauthClient.getUserInfo(code);

        mergeAccount(primaryUserId, secondaryUserId, provider, userInfo.providerId());
    }
}
