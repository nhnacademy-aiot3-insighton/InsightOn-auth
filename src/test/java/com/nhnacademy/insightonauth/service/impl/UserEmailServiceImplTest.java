package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.dto.auth.UserLoginResult;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.exception.user.*;
import com.nhnacademy.insightonauth.exception.email.*;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.redis.ResendCounter;
import com.nhnacademy.insightonauth.repository.UserRepository;
import com.nhnacademy.insightonauth.service.EmailVerificationService;
import com.nhnacademy.insightonauth.service.TokenService;
import com.nhnacademy.insightonauth.service.UserCredentialService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEmailServiceImplTest {

    @Mock private EmailVerificationService emailVerificationService;
    @Mock private RedisService redisService;
    @Mock private ResendCounter resendCounter;
    @Mock private TokenService tokenService;
    @Mock private UserManagementService userManagementService;
    @Mock private UserCredentialService userCredentialService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserEmailServiceImpl userEmailService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
        ReflectionTestUtils.setField(user, "userId", 1L);
    }

    // ---------- emailVerifyRequest ----------

    @Test
    @DisplayName("emailVerifyRequest - 재전송 잠금이면 예외")
    void emailVerifyRequest_locked() {
        when(redisService.hasKey(contains("verify-resend-lock"))).thenReturn(true);

        assertThatThrownBy(() -> userEmailService.emailVerifyRequest("test@test.com"))
                .isInstanceOf(VerificationResendLockedException.class);
    }

    @Test
    @DisplayName("emailVerifyRequest - 쿨다운이면 예외")
    void emailVerifyRequest_cooldown() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(redisService.setIfAbsent(contains("verify-resend-cooldown"), anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> userEmailService.emailVerifyRequest("test@test.com"))
                .isInstanceOf(VerificationResendTooSoonException.class);
    }

    @Test
    @DisplayName("emailVerifyRequest - 이번 요청으로 잠기면 예외")
    void emailVerifyRequest_lockedNow() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(redisService.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(resendCounter.increase(anyString(), anyString(), anyInt(), any())).thenReturn(true);

        assertThatThrownBy(() -> userEmailService.emailVerifyRequest("test@test.com"))
                .isInstanceOf(VerificationResendLockedException.class);
    }

    @Test
    @DisplayName("emailVerifyRequest - 정상이면 인증 코드 발송")
    void emailVerifyRequest_success() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(redisService.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(resendCounter.increase(anyString(), anyString(), anyInt(), any())).thenReturn(false);

        userEmailService.emailVerifyRequest("test@test.com");

        verify(emailVerificationService).sendVerificationCode("test@test.com");
    }

    // ---------- reactivateRequest ----------

    @Test
    @DisplayName("reactivateRequest - 쿨다운이면 예외")
    void reactivateRequest_cooldown() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(redisService.setIfAbsent(contains("reactive-resend-cooldown"), anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> userEmailService.reactivateRequest("test@test.com"))
                .isInstanceOf(VerificationResendTooSoonException.class);
        verify(emailVerificationService, never()).sendReactiveVerificationCode(anyString());
    }

    @Test
    @DisplayName("reactivateRequest - 복구 가능한 계정이 없어도 예외 없이 종료 (메일만 미발송)")
    void reactivateRequest_noAccount() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(redisService.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(resendCounter.increase(anyString(), anyString(), anyInt(), any())).thenReturn(false);
        when(userManagementService.findReactivatableByEmail("test@test.com")).thenReturn(Optional.empty());

        userEmailService.reactivateRequest("test@test.com");

        verify(emailVerificationService, never()).sendReactiveVerificationCode(anyString());
    }

    @Test
    @DisplayName("reactivateRequest - 복구 가능한 계정이면 재활성화 코드 발송")
    void reactivateRequest_success() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(redisService.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(resendCounter.increase(anyString(), anyString(), anyInt(), any())).thenReturn(false);
        when(userManagementService.findReactivatableByEmail("test@test.com")).thenReturn(Optional.of(user));

        userEmailService.reactivateRequest("test@test.com");

        verify(emailVerificationService).sendReactiveVerificationCode("test@test.com");
    }

    // ---------- reactivateConfirm ----------

    @Test
    @DisplayName("reactivateConfirm - 계정을 못 찾으면 예외")
    void reactivateConfirm_notFound() {
        when(userManagementService.findReactivatableByEmail("test@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userEmailService.reactivateConfirm("test@test.com", "code"))
                .isInstanceOf(UserNotFoundException.class);
        verify(emailVerificationService).emailReactiveVerifyCheck("test@test.com", "code");
    }

    @Test
    @DisplayName("reactivateConfirm - 성공 시 재활성화 + 토큰 발급")
    void reactivateConfirm_success() {
        when(userManagementService.findReactivatableByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(tokenService.issueTokens(eq(user), anyString()))
                .thenReturn(UserLoginResult.success("access", "refresh"));

        UserLoginResult result = userEmailService.reactivateConfirm("test@test.com", "code");

        verify(userManagementService).reactivate(user);
        assertThat(result.accessToken()).isEqualTo("access");
    }

    // ---------- passwordResetRequest ----------

    @Test
    @DisplayName("passwordResetRequest - 잠겨있으면 예외")
    void passwordResetRequest_locked() {
        when(redisService.hasKey(contains("password-reset-resend-lock"))).thenReturn(true);

        assertThatThrownBy(() -> userEmailService.passwordResetRequest("test@test.com"))
                .isInstanceOf(PasswordResetResendLockedException.class);
    }

    @Test
    @DisplayName("passwordResetRequest - 쿨다운이면 예외")
    void passwordResetRequest_tooSoon() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(redisService.setIfAbsent(contains("password-reset-resend-cooldown"), anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> userEmailService.passwordResetRequest("test@test.com"))
                .isInstanceOf(PasswordResetResendTooSoonException.class);
    }

    @Test
    @DisplayName("passwordResetRequest - 이번 요청으로 잠기면 예외")
    void passwordResetRequest_lockedNow() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(redisService.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(resendCounter.increase(anyString(), anyString(), anyInt(), any())).thenReturn(true);

        assertThatThrownBy(() -> userEmailService.passwordResetRequest("test@test.com"))
                .isInstanceOf(PasswordResetResendLockedException.class);
    }

    @Test
    @DisplayName("passwordResetRequest - 계정이 없어도 예외 없이 종료 (메일만 미발송)")
    void passwordResetRequest_noAccount() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(redisService.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(resendCounter.increase(anyString(), anyString(), anyInt(), any())).thenReturn(false);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        userEmailService.passwordResetRequest("test@test.com");

        verify(emailVerificationService, never()).sendPasswordResetPath(anyString());
    }

    @Test
    @DisplayName("passwordResetRequest - 탈퇴 계정이면 메일 미발송")
    void passwordResetRequest_withdrawnAccount() {
        user.setStatus(Status.WITHDRAW);
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(redisService.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(resendCounter.increase(anyString(), anyString(), anyInt(), any())).thenReturn(false);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        userEmailService.passwordResetRequest("test@test.com");

        verify(emailVerificationService, never()).sendPasswordResetPath(anyString());
    }

    @Test
    @DisplayName("passwordResetRequest - 정상 계정이면 재설정 메일 발송")
    void passwordResetRequest_activeAccount() {
        when(redisService.hasKey(anyString())).thenReturn(false);
        when(redisService.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(resendCounter.increase(anyString(), anyString(), anyInt(), any())).thenReturn(false);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        userEmailService.passwordResetRequest("test@test.com");

        verify(emailVerificationService).sendPasswordResetPath("test@test.com");
    }

    // ---------- passwordResetConfirm ----------

    @Test
    @DisplayName("passwordResetConfirm - 토큰 검증 후 비밀번호 변경")
    void passwordResetConfirm() {
        when(emailVerificationService.emailTokenVerify("token")).thenReturn("test@test.com");
        when(userManagementService.findByEmail("test@test.com")).thenReturn(user);

        userEmailService.passwordResetConfirm("token", "newPw");

        verify(userCredentialService).updatePassword(any(), eq(user), eq("newPw"));
        verify(emailVerificationService).consumePasswordResetToken("token", "test@test.com");
    }

    @Test
    @DisplayName("passwordResetConfirm - 비밀번호 변경 실패 시 토큰을 소모하지 않는다 (재시도 가능)")
    void passwordResetConfirm_updateFails_tokenNotConsumed() {
        when(emailVerificationService.emailTokenVerify("token")).thenReturn("test@test.com");
        when(userManagementService.findByEmail("test@test.com")).thenReturn(user);
        doThrow(new SameAsOldPasswordException("새 비밀번호는 기존 비밀번호와 달라야 합니다."))
                .when(userCredentialService).updatePassword(any(), eq(user), eq("newPw"));

        assertThatThrownBy(() -> userEmailService.passwordResetConfirm("token", "newPw"))
                .isInstanceOf(SameAsOldPasswordException.class);

        verify(emailVerificationService, never()).consumePasswordResetToken(anyString(), anyString());
    }
}
