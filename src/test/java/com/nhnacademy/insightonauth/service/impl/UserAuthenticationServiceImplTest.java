package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.client.OauthClient;
import com.nhnacademy.insightonauth.client.OauthClientResolver;
import com.nhnacademy.insightonauth.dto.auth.TokenRefreshResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResult;
import com.nhnacademy.insightonauth.dto.oauth.OauthUserInfo;
import com.nhnacademy.insightonauth.entity.Oauth;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserCredential;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.exception.auth.*;
import com.nhnacademy.insightonauth.exception.user.*;
import com.nhnacademy.insightonauth.exception.signup.*;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.redis.ResendCounter;
import com.nhnacademy.insightonauth.repository.UserRepository;
import com.nhnacademy.insightonauth.service.*;
import io.jsonwebtoken.JwtException;
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
class UserAuthenticationServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserCredentialService userCredentialService;
    @Mock private UserRoleService userRoleService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private RedisService redisService;
    @Mock private OauthClientResolver oauthClientResolver;
    @Mock private OauthService oauthService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private TokenService tokenService;
    @Mock private UserManagementService userManagementService;
    @Mock private ResendCounter resendCounter;
    @Mock private OauthClient oauthClient;

    @InjectMocks
    private UserAuthenticationServiceImpl authService;

    private User user;
    private UserCredential credential;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
        ReflectionTestUtils.setField(user, "userId", 1L);
        credential = mock(UserCredential.class);
    }

    // ---------- login ----------

    @Test
    @DisplayName("login - 계정이 잠겨 있으면 예외")
    void login_locked() {
        when(redisService.hasKey(contains("login-lock"))).thenReturn(true);

        assertThatThrownBy(() -> authService.login("test@test.com", "pw"))
                .isInstanceOf(LoginTemporarilyLockedException.class);
    }

    @Test
    @DisplayName("login - 계정을 못 찾으면 InvalidCredentials")
    void login_notFound() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(userManagementService.findReactivatableByEmail("test@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("test@test.com", "pw"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login - 비밀번호가 틀리면 InvalidCredentials + 실패 카운트")
    void login_wrongPassword() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(userManagementService.findReactivatableByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userCredentialService.findByUser(user)).thenReturn(credential);
        when(credential.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("pw", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("test@test.com", "pw"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(resendCounter).increase(anyString(), anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("login - 탈퇴 계정이 복구 기간 내면 복구 유도 응답")
    void login_withdrawnWithinPeriod() {
        user.setStatus(Status.WITHDRAW);
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(userManagementService.findReactivatableByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userCredentialService.findByUser(user)).thenReturn(credential);
        when(credential.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(tokenService.isWithinRestorePeriod(user)).thenReturn(true);

        UserLoginResult result = authService.login("test@test.com", "pw");

        assertThat(result.status()).isEqualTo("PENDING_RESTORE");
    }

    @Test
    @DisplayName("login - 탈퇴 계정이 복구 기간을 지났으면 예외")
    void login_withdrawnExpired() {
        user.setStatus(Status.WITHDRAW);
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(userManagementService.findReactivatableByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userCredentialService.findByUser(user)).thenReturn(credential);
        when(credential.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(tokenService.isWithinRestorePeriod(user)).thenReturn(false);

        assertThatThrownBy(() -> authService.login("test@test.com", "pw"))
                .isInstanceOf(RestorePeriodExpiredException.class);
    }

    @Test
    @DisplayName("login - 정지 등 로그인 불가 상태면 InvalidUserException")
    void login_notLoginable() {
        user.setStatus(Status.BLOCK);
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(userManagementService.findReactivatableByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userCredentialService.findByUser(user)).thenReturn(credential);
        when(credential.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("test@test.com", "pw"))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    @DisplayName("login - 휴면 계정이면 자동 해제 후 그대로 로그인")
    void login_sleepAutoReactivate() {
        user.setStatus(Status.SLEEP);
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(userManagementService.findReactivatableByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userCredentialService.findByUser(user)).thenReturn(credential);
        when(credential.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        // reactivate 는 mock 이므로 실제 상태 전환을 흉내낸다 (안 그러면 아래 isLoginable 체크에 걸림)
        doAnswer(inv -> { user.setStatus(Status.ACTIVE); return null; })
                .when(userManagementService).reactivate(user);
        when(tokenService.issueTokens(user, "test@test.com"))
                .thenReturn(UserLoginResult.success("access", "refresh"));

        UserLoginResult result = authService.login("test@test.com", "pw");

        verify(userManagementService).reactivate(user);
        assertThat(result.accessToken()).isEqualTo("access");
    }

    @Test
    @DisplayName("login - 정상 계정이면 토큰 발급 + lastLoginAt 갱신")
    void login_success() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(userManagementService.findReactivatableByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userCredentialService.findByUser(user)).thenReturn(credential);
        when(credential.getPasswordHash()).thenReturn("hash");
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(tokenService.issueTokens(user, "test@test.com"))
                .thenReturn(UserLoginResult.success("access", "refresh"));

        UserLoginResult result = authService.login("test@test.com", "pw");

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    // ---------- oauthLogin ----------

    private OauthUserInfo userInfo() {
        return new OauthUserInfo("test@test.com", "test", "pid-1");
    }

    @Test
    @DisplayName("oauthLogin - 기존 활성 연동이면 토큰 발급")
    void oauthLogin_existingActive() {
        when(oauthClientResolver.resolve("google")).thenReturn(oauthClient);
        when(oauthClient.getUserInfo("code")).thenReturn(userInfo());
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1"))
                .thenReturn(Optional.of(new Oauth(user, "google", "pid-1")));
        when(tokenService.issueTokens(user, "test@test.com"))
                .thenReturn(UserLoginResult.success("access", "refresh"));

        UserLoginResult result = authService.oauthLogin("google", "code");

        assertThat(result.accessToken()).isEqualTo("access");
    }

    @Test
    @DisplayName("oauthLogin - 기존 연동이 휴면이면 자동 해제 후 그대로 로그인")
    void oauthLogin_existingSleepAutoReactivate() {
        user.setStatus(Status.SLEEP);
        when(oauthClientResolver.resolve("google")).thenReturn(oauthClient);
        when(oauthClient.getUserInfo("code")).thenReturn(userInfo());
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1"))
                .thenReturn(Optional.of(new Oauth(user, "google", "pid-1")));
        doAnswer(inv -> { user.setStatus(Status.ACTIVE); return null; })
                .when(userManagementService).reactivate(user);
        when(tokenService.issueTokens(user, "test@test.com"))
                .thenReturn(UserLoginResult.success("access", "refresh"));

        UserLoginResult result = authService.oauthLogin("google", "code");

        verify(userManagementService).reactivate(user);
        assertThat(result.accessToken()).isEqualTo("access");
    }

    @Test
    @DisplayName("oauthLogin - 기존 연동이 탈퇴+복구기간 내면 복구 유도")
    void oauthLogin_existingWithdrawnWithinPeriod() {
        user.setStatus(Status.WITHDRAW);
        when(oauthClientResolver.resolve("google")).thenReturn(oauthClient);
        when(oauthClient.getUserInfo("code")).thenReturn(userInfo());
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1"))
                .thenReturn(Optional.of(new Oauth(user, "google", "pid-1")));
        when(tokenService.isWithinRestorePeriod(user)).thenReturn(true);

        assertThat(authService.oauthLogin("google", "code").status()).isEqualTo("PENDING_RESTORE");
    }

    @Test
    @DisplayName("oauthLogin - 기존 연동이 탈퇴+복구기간 지났으면 마스킹 후 새 계정으로 가입")
    void oauthLogin_existingWithdrawnExpired() {
        user.setStatus(Status.WITHDRAW);
        when(oauthClientResolver.resolve("google")).thenReturn(oauthClient);
        when(oauthClient.getUserInfo("code")).thenReturn(userInfo());
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1"))
                .thenReturn(Optional.of(new Oauth(user, "google", "pid-1")));
        when(tokenService.isWithinRestorePeriod(user)).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(tokenService.issueTokens(any(User.class), eq("test@test.com")))
                .thenReturn(UserLoginResult.success("access", "refresh"));

        UserLoginResult result = authService.oauthLogin("google", "code");

        assertThat(result.accessToken()).isEqualTo("access");
        verify(oauthService).maskByUser(user);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("oauthLogin - 연동 없고 마스킹된 연동이 복구기간 내면 복구 유도")
    void oauthLogin_reactivatablePendingWithinPeriod() {
        User maskedOwner = new User("masked@test.com", "가려짐", null);
        when(oauthClientResolver.resolve("google")).thenReturn(oauthClient);
        when(oauthClient.getUserInfo("code")).thenReturn(userInfo());
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1")).thenReturn(Optional.empty());
        when(oauthService.findReactivatableByProviderAndProviderUserId("google", "pid-1"))
                .thenReturn(Optional.of(new Oauth(maskedOwner, "google", "pid-1;masked")));
        when(tokenService.isWithinRestorePeriod(maskedOwner)).thenReturn(true);

        assertThat(authService.oauthLogin("google", "code").status()).isEqualTo("PENDING_RESTORE");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("oauthLogin - 연동 없고 이메일 이미 가입돼 있으면 예외")
    void oauthLogin_emailAlreadyRegistered() {
        when(oauthClientResolver.resolve("google")).thenReturn(oauthClient);
        when(oauthClient.getUserInfo("code")).thenReturn(userInfo());
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1")).thenReturn(Optional.empty());
        when(oauthService.findReactivatableByProviderAndProviderUserId("google", "pid-1"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.oauthLogin("google", "code"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    @DisplayName("oauthLogin - 신규 유저면 생성 + 역할/연동 등록 + 토큰 발급")
    void oauthLogin_newUser() {
        when(oauthClientResolver.resolve("google")).thenReturn(oauthClient);
        when(oauthClient.getUserInfo("code")).thenReturn(userInfo());
        when(oauthService.findByProviderAndProviderUserId("google", "pid-1")).thenReturn(Optional.empty());
        when(oauthService.findReactivatableByProviderAndProviderUserId("google", "pid-1"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(tokenService.issueTokens(any(User.class), eq("test@test.com")))
                .thenReturn(UserLoginResult.success("access", "refresh"));

        UserLoginResult result = authService.oauthLogin("google", "code");

        assertThat(result.accessToken()).isEqualTo("access");
        verify(userRepository).save(any(User.class));
        verify(userRoleService).create(any(User.class), eq(Role.MEMBER));
        verify(oauthService).create(any(User.class), eq("google"), eq("pid-1"));
    }

    // ---------- logout / refresh ----------

    @Test
    @DisplayName("logout - refresh 삭제 + 블랙리스트 등록")
    void logout() {
        authService.logout(1L, "access-token");

        verify(redisService).delete(contains("refresh"));
        verify(tokenBlacklistService).blacklistToken("access-token");
    }

    @Test
    @DisplayName("forceLogout - 현재 access 토큰 무효화 + refresh 삭제")
    void forceLogout() {
        authService.forceLogout(1L);

        verify(tokenBlacklistService).blacklistByUserId(1L);
        verify(redisService).delete(contains("refresh"));
    }

    @Test
    @DisplayName("refresh - 토큰 검증 실패면 예외")
    void refresh_invalidToken() {
        doThrow(new JwtException("bad")).when(jwtProvider).validateRefreshToken(1L, "rt");

        assertThatThrownBy(() -> authService.refresh(1L, "rt"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("refresh - 로그인 불가 상태면 예외")
    void refresh_notLoginable() {
        user.setStatus(Status.BLOCK);
        when(userManagementService.findById(1L)).thenReturn(user);

        assertThatThrownBy(() -> authService.refresh(1L, "rt"))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    @DisplayName("refresh - 정상이면 새 access token 발급")
    void refresh_success() {
        when(userManagementService.findById(1L)).thenReturn(user);
        when(userRoleService.findByUser(user)).thenReturn(List.of(new UserRole(user, Role.MEMBER)));
        when(jwtProvider.createAccessToken(1L, List.of("MEMBER"), "test")).thenReturn("new-access");

        TokenRefreshResponse result = authService.refresh(1L, "rt");

        assertThat(result.accessToken()).isEqualTo("new-access");
    }
}
