package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.client.OauthClient;
import com.nhnacademy.insightonauth.client.OauthClientResolver;
import com.nhnacademy.insightonauth.dto.core.UserGroupResponse;
import com.nhnacademy.insightonauth.dto.mypage.MyInfoResponse;
import com.nhnacademy.insightonauth.dto.oauth.OauthUserInfo;
import com.nhnacademy.insightonauth.entity.Oauth;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserCredential;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.exception.*;
import com.nhnacademy.insightonauth.exception.auth.*;
import com.nhnacademy.insightonauth.exception.user.*;
import com.nhnacademy.insightonauth.exception.email.*;
import com.nhnacademy.insightonauth.exception.signup.*;
import com.nhnacademy.insightonauth.exception.oauth.*;
import com.nhnacademy.insightonauth.exception.external.*;
import com.nhnacademy.insightonauth.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyPageServiceImplTest {

    @Mock private UserCredentialService userCredentialService;
    @Mock private UserRoleService userRoleService;
    @Mock private OauthService oauthService;
    @Mock private OauthClientResolver oauthClientResolver;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserManagementService userManagementService;
    @Mock private CoreService coreService;
    @Mock private OauthClient oauthClient;

    @InjectMocks
    private MyPageServiceImpl myPageService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
        ReflectionTestUtils.setField(user, "userId", 1L);
    }

    // ---------- findMyInfo ----------

    @Test
    @DisplayName("findMyInfo - 그룹이 있으면 그룹명 포함")
    void findMyInfo_withGroup() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(coreService.getUserGroup(1L)).thenReturn(new UserGroupResponse(true, "그룹A"));

        MyInfoResponse result = myPageService.findMyInfo(1L);

        assertThat(result.groupName()).isEqualTo("그룹A");
    }

    @Test
    @DisplayName("findMyInfo - 그룹이 없으면 '그룹 없음'")
    void findMyInfo_noGroup() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(coreService.getUserGroup(1L)).thenReturn(new UserGroupResponse(false, null));

        MyInfoResponse result = myPageService.findMyInfo(1L);

        assertThat(result.groupName()).isEqualTo("그룹 없음");
    }

    // ---------- updatePassword ----------

    @Test
    @DisplayName("updatePassword - 현재 비밀번호가 틀리면 예외")
    void updatePassword_wrongCurrent() {
        UserCredential credential = mock(UserCredential.class);
        when(userManagementService.findById(1L)).thenReturn(user);
        when(userCredentialService.findByUser(user)).thenReturn(credential);
        when(credential.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("cur", "hash")).thenReturn(false);

        assertThatThrownBy(() -> myPageService.updatePassword(1L, "cur", "new"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("updatePassword - 현재 비밀번호가 맞으면 updatePassword에 위임 (기존과 동일 여부는 그쪽에서 검사)")
    void updatePassword_success() {
        UserCredential credential = mock(UserCredential.class);
        when(userManagementService.findById(1L)).thenReturn(user);
        when(userCredentialService.findByUser(user)).thenReturn(credential);
        when(credential.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("cur", "hash")).thenReturn(true);

        myPageService.updatePassword(1L, "cur", "new");

        verify(userCredentialService).updatePassword(any(), eq(user), eq("new"));
    }

    // ---------- findMyRoles / findMyOauths ----------

    @Test
    @DisplayName("findMyRoles - 역할 목록 매핑")
    void findMyRoles() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(userRoleService.findByUser(user)).thenReturn(List.of(new UserRole(user, Role.MEMBER)));

        assertThat(myPageService.findMyRoles(1L)).hasSize(1);
    }

    @Test
    @DisplayName("findMyOauths - 연동 목록 매핑")
    void findMyOauths() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(oauthService.findAllByUser(user)).thenReturn(List.of(new Oauth(user, "google", "pid-1")));

        assertThat(myPageService.findMyOauths(1L)).hasSize(1);
    }

    // ---------- linkOauth ----------

    private OauthUserInfo userInfo() {
        return new OauthUserInfo("test@test.com", "test", "pid-1");
    }

    @Test
    @DisplayName("linkOauth - 이미 같은 provider 연동돼 있으면 외부 OAuth 호출 없이 예외")
    void linkOauth_providerAlreadyLinked() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(oauthService.hasProviderLinked(user, "google")).thenReturn(true);

        assertThatThrownBy(() -> myPageService.linkOauth(1L, "google", "code"))
                .isInstanceOf(OauthAlreadyLinkedException.class);

        verifyNoInteractions(oauthClientResolver, oauthClient);
    }

    @Test
    @DisplayName("linkOauth - 이미 내 계정에 연동돼 있으면 예외")
    void linkOauth_alreadyLinked() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(oauthClientResolver.resolve("google")).thenReturn(oauthClient);
        when(oauthClient.getUserInfo("code")).thenReturn(userInfo());
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1"))
                .thenReturn(Optional.of(new Oauth(user, "google", "pid-1")));

        assertThatThrownBy(() -> myPageService.linkOauth(1L, "google", "code"))
                .isInstanceOf(OauthAlreadyLinkedException.class);
    }

    @Test
    @DisplayName("linkOauth - 다른 계정에 연동돼 있으면 예외")
    void linkOauth_otherAccount() {
        User other = new User("other@test.com", "other", "01099998888");
        ReflectionTestUtils.setField(other, "userId", 2L);
        when(userManagementService.findById(1L)).thenReturn(user);
        when(oauthClientResolver.resolve("google")).thenReturn(oauthClient);
        when(oauthClient.getUserInfo("code")).thenReturn(userInfo());
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1"))
                .thenReturn(Optional.of(new Oauth(other, "google", "pid-1")));

        assertThatThrownBy(() -> myPageService.linkOauth(1L, "google", "code"))
                .isInstanceOf(OauthLinkedToOtherAccountException.class);
    }

    @Test
    @DisplayName("linkOauth - 충돌 없으면 연동 생성")
    void linkOauth_success() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(oauthClientResolver.resolve("google")).thenReturn(oauthClient);
        when(oauthClient.getUserInfo("code")).thenReturn(userInfo());
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1")).thenReturn(Optional.empty());

        myPageService.linkOauth(1L, "google", "code");

        verify(oauthService).create(user, "google", "pid-1");
    }

    // ---------- mergeAccount ----------

    @Test
    @DisplayName("mergeAccount - 자기 자신과 병합하면 예외")
    void mergeAccount_self() {
        assertThatThrownBy(() -> myPageService.mergeAccount(1L, 1L, "google", "pid-1"))
                .isInstanceOf(InvalidMergeRequestException.class);
    }

    @Test
    @DisplayName("mergeAccount - 연동 정보가 없으면 예외")
    void mergeAccount_oauthNotFound() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> myPageService.mergeAccount(1L, 2L, "google", "pid-1"))
                .isInstanceOf(OauthNotFoundException.class);
    }

    @Test
    @DisplayName("mergeAccount - secondary 소유자가 다르면 예외")
    void mergeAccount_ownerMismatch() {
        User secondary = new User("s@test.com", "s", "01055556666");
        ReflectionTestUtils.setField(secondary, "userId", 3L);
        when(userManagementService.findById(1L)).thenReturn(user);
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1"))
                .thenReturn(Optional.of(new Oauth(secondary, "google", "pid-1")));

        assertThatThrownBy(() -> myPageService.mergeAccount(1L, 2L, "google", "pid-1"))
                .isInstanceOf(InvalidMergeRequestException.class);
    }

    @Test
    @DisplayName("mergeAccount - 정상이면 연동 이관 + secondary 계정 삭제")
    void mergeAccount_success() {
        User secondary = new User("s@test.com", "s", "01055556666");
        ReflectionTestUtils.setField(secondary, "userId", 2L);
        Oauth oauth = new Oauth(secondary, "google", "pid-1");
        when(userManagementService.findById(1L)).thenReturn(user);
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1")).thenReturn(Optional.of(oauth));

        myPageService.mergeAccount(1L, 2L, "google", "pid-1");

        assertThat(oauth.getUser()).isEqualTo(user);
        verify(userManagementService).deleteUser(2L);
    }
}
