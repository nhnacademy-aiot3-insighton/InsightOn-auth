package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.email.SmtpMailSender;
import com.nhnacademy.insightonauth.exception.email.InvalidVerificationCodeException;
import com.nhnacademy.insightonauth.exception.email.InvalidVerificationTokenException;
import com.nhnacademy.insightonauth.exception.email.VerificationTemporarilyLockedException;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock
    private SmtpMailSender mailSender;
    @Mock
    private RedisService redisService;

    @InjectMocks
    private EmailVerificationServiceImpl emailVerificationService;

    // ---------- sendVerificationCode ----------

    @Test
    @DisplayName("sendVerificationCode - 코드를 Redis에 저장하고 메일 발송")
    void sendVerificationCode() {
        emailVerificationService.sendVerificationCode("test@test.com");

        verify(redisService).set(eq(RedisKey.VERIFY.getPrefix() + "test@test.com"), anyString(), eq(Duration.ofMinutes(5)));
        verify(mailSender).send(eq("test@test.com"), anyString(), anyString());
    }

    // ---------- sendReactiveVerificationCode ----------

    @Test
    @DisplayName("sendReactiveVerificationCode - REACTIVE 키로 저장하고 메일 발송")
    void sendReactiveVerificationCode() {
        emailVerificationService.sendReactiveVerificationCode("test@test.com");

        verify(redisService).set(eq(RedisKey.REACTIVE.getPrefix() + "test@test.com"), anyString(), eq(Duration.ofMinutes(5)));
        verify(mailSender).send(eq("test@test.com"), anyString(), anyString());
    }

    // ---------- sendPasswordResetPath ----------

    @Test
    @DisplayName("sendPasswordResetPath - 기존 토큰이 없으면 새 토큰만 저장")
    void sendPasswordResetPath_noOldToken() {
        when(redisService.get(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + "test@test.com")).thenReturn(null);

        emailVerificationService.sendPasswordResetPath("test@test.com");

        verify(redisService, never()).delete(startsWith(RedisKey.PASSWORD_RESET.getPrefix()));
        verify(redisService).set(startsWith(RedisKey.PASSWORD_RESET.getPrefix()), eq("test@test.com"), eq(Duration.ofMinutes(10)));
        verify(redisService).set(eq(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + "test@test.com"), anyString(), eq(Duration.ofMinutes(10)));
        verify(mailSender).send(eq("test@test.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("sendPasswordResetPath - 기존 토큰이 있으면 예전 링크를 무효화")
    void sendPasswordResetPath_withOldToken() {
        when(redisService.get(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + "test@test.com")).thenReturn("old-uuid");

        emailVerificationService.sendPasswordResetPath("test@test.com");

        verify(redisService).delete(RedisKey.PASSWORD_RESET.getPrefix() + "old-uuid");
    }

    // ---------- emailCodeVerify ----------

    @Test
    @DisplayName("emailCodeVerify - 잠겨있으면 예외")
    void emailCodeVerify_locked() {
        when(redisService.hasKey(RedisKey.VERIFY_FAIL_LOCK.getPrefix() + "test@test.com")).thenReturn(true);

        assertThatThrownBy(() -> emailVerificationService.emailCodeVerify("test@test.com", "123456"))
                .isInstanceOf(VerificationTemporarilyLockedException.class);
    }

    @Test
    @DisplayName("emailCodeVerify - 코드가 일치하면 검증 토큰 발급 및 실패카운트 초기화")
    void emailCodeVerify_success() {
        when(redisService.hasKey(RedisKey.VERIFY_FAIL_LOCK.getPrefix() + "test@test.com")).thenReturn(false);
        when(redisService.get(RedisKey.VERIFY.getPrefix() + "test@test.com")).thenReturn("123456");

        String token = emailVerificationService.emailCodeVerify("test@test.com", "123456");

        assertThat(token).isNotBlank();
        verify(redisService).delete(RedisKey.VERIFY_FAIL.getPrefix() + "test@test.com");
        verify(redisService).delete(RedisKey.VERIFY.getPrefix() + "test@test.com");
        verify(redisService).set(RedisKey.VERIFIED.getPrefix() + "test@test.com", token, Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("emailCodeVerify - 코드가 틀리면 예외, 5회 미만이면 잠기지 않음")
    void emailCodeVerify_wrongCode_belowThreshold() {
        when(redisService.hasKey(RedisKey.VERIFY_FAIL_LOCK.getPrefix() + "test@test.com")).thenReturn(false);
        when(redisService.get(RedisKey.VERIFY.getPrefix() + "test@test.com")).thenReturn("123456");
        when(redisService.increment(RedisKey.VERIFY_FAIL.getPrefix() + "test@test.com", Duration.ofMinutes(5))).thenReturn(3L);

        assertThatThrownBy(() -> emailVerificationService.emailCodeVerify("test@test.com", "000000"))
                .isInstanceOf(InvalidVerificationCodeException.class);
        verify(redisService, never()).set(eq(RedisKey.VERIFY_FAIL_LOCK.getPrefix() + "test@test.com"), anyString(), any());
    }

    @Test
    @DisplayName("emailCodeVerify - 5회째 틀리면 잠금 설정")
    void emailCodeVerify_wrongCode_reachesThreshold() {
        when(redisService.hasKey(RedisKey.VERIFY_FAIL_LOCK.getPrefix() + "test@test.com")).thenReturn(false);
        when(redisService.get(RedisKey.VERIFY.getPrefix() + "test@test.com")).thenReturn("123456");
        when(redisService.increment(RedisKey.VERIFY_FAIL.getPrefix() + "test@test.com", Duration.ofMinutes(5))).thenReturn(5L);

        // 임계치 도달 시엔 increaseVerifyFailCount 자체가 잠금 예외를 던지고 끝남 (뒤의 InvalidVerificationCodeException까지 안 감)
        assertThatThrownBy(() -> emailVerificationService.emailCodeVerify("test@test.com", "000000"))
                .isInstanceOf(VerificationTemporarilyLockedException.class);
        verify(redisService).set(RedisKey.VERIFY_FAIL_LOCK.getPrefix() + "test@test.com", "locked", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("emailCodeVerify - 저장된 코드가 없으면 예외")
    void emailCodeVerify_noSavedCode() {
        when(redisService.hasKey(RedisKey.VERIFY_FAIL_LOCK.getPrefix() + "test@test.com")).thenReturn(false);
        when(redisService.get(RedisKey.VERIFY.getPrefix() + "test@test.com")).thenReturn(null);
        when(redisService.increment(anyString(), any())).thenReturn(1L);

        assertThatThrownBy(() -> emailVerificationService.emailCodeVerify("test@test.com", "123456"))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }

    // ---------- emailVerifyCheck ----------

    @Test
    @DisplayName("emailVerifyCheck - 토큰이 일치하면 true 반환 후 삭제")
    void emailVerifyCheck_success() {
        when(redisService.get(RedisKey.VERIFIED.getPrefix() + "test@test.com")).thenReturn("verify-token");

        boolean result = emailVerificationService.emailVerifyCheck("test@test.com", "verify-token");

        assertThat(result).isTrue();
        verify(redisService).delete(RedisKey.VERIFIED.getPrefix() + "test@test.com");
    }

    @Test
    @DisplayName("emailVerifyCheck - 토큰이 다르면 예외")
    void emailVerifyCheck_mismatch() {
        when(redisService.get(RedisKey.VERIFIED.getPrefix() + "test@test.com")).thenReturn("verify-token");

        assertThatThrownBy(() -> emailVerificationService.emailVerifyCheck("test@test.com", "wrong-token"))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }

    @Test
    @DisplayName("emailVerifyCheck - 저장된 토큰이 없으면 예외")
    void emailVerifyCheck_noSavedToken() {
        when(redisService.get(RedisKey.VERIFIED.getPrefix() + "test@test.com")).thenReturn(null);

        assertThatThrownBy(() -> emailVerificationService.emailVerifyCheck("test@test.com", "any-token"))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }

    // ---------- emailTokenVerify ----------

    @Test
    @DisplayName("emailTokenVerify - 유효한 토큰이면 이메일을 반환하고 키는 지우지 않는다 (비밀번호 변경 성공 후에만 소모)")
    void emailTokenVerify_success() {
        when(redisService.get(RedisKey.PASSWORD_RESET.getPrefix() + "reset-token")).thenReturn("test@test.com");

        String email = emailVerificationService.emailTokenVerify("reset-token");

        assertThat(email).isEqualTo("test@test.com");
        verify(redisService, never()).delete(anyString());
    }

    @Test
    @DisplayName("emailTokenVerify - 존재하지 않는 토큰이면 예외")
    void emailTokenVerify_notFound() {
        when(redisService.get(RedisKey.PASSWORD_RESET.getPrefix() + "bad-token")).thenReturn(null);

        assertThatThrownBy(() -> emailVerificationService.emailTokenVerify("bad-token"))
                .isInstanceOf(InvalidVerificationTokenException.class);
    }

    // ---------- consumePasswordResetToken ----------

    @Test
    @DisplayName("consumePasswordResetToken - 정방향/역방향 키 모두 삭제")
    void consumePasswordResetToken_success() {
        when(redisService.get(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + "test@test.com")).thenReturn("reset-token");

        emailVerificationService.consumePasswordResetToken("reset-token", "test@test.com");

        verify(redisService).delete(RedisKey.PASSWORD_RESET.getPrefix() + "reset-token");
        verify(redisService).delete(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + "test@test.com");
    }

    @Test
    @DisplayName("consumePasswordResetToken - 역방향 키가 이미 다른 토큰을 가리키면 역방향은 안 지움")
    void consumePasswordResetToken_reverseKeyAlreadyRotated() {
        when(redisService.get(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + "test@test.com")).thenReturn("newer-token");

        emailVerificationService.consumePasswordResetToken("reset-token", "test@test.com");

        verify(redisService, never()).delete(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + "test@test.com");
    }

    // ---------- emailReactiveVerifyCheck ----------

    @Test
    @DisplayName("emailReactiveVerifyCheck - 잠겨있으면 예외")
    void emailReactiveVerifyCheck_locked() {
        when(redisService.hasKey(RedisKey.REACTIVE_VERIFY_FAIL_LOCK.getPrefix() + "test@test.com")).thenReturn(true);

        assertThatThrownBy(() -> emailVerificationService.emailReactiveVerifyCheck("test@test.com", "123456"))
                .isInstanceOf(VerificationTemporarilyLockedException.class);
    }

    @Test
    @DisplayName("emailReactiveVerifyCheck - 코드가 일치하면 성공 처리")
    void emailReactiveVerifyCheck_success() {
        when(redisService.hasKey(RedisKey.REACTIVE_VERIFY_FAIL_LOCK.getPrefix() + "test@test.com")).thenReturn(false);
        when(redisService.get(RedisKey.REACTIVE.getPrefix() + "test@test.com")).thenReturn("123456");

        emailVerificationService.emailReactiveVerifyCheck("test@test.com", "123456");

        verify(redisService).delete(RedisKey.REACTIVE_VERIFY_FAIL.getPrefix() + "test@test.com");
        verify(redisService).delete(RedisKey.REACTIVE.getPrefix() + "test@test.com");
    }

    @Test
    @DisplayName("emailReactiveVerifyCheck - 코드가 틀리면 예외")
    void emailReactiveVerifyCheck_wrongCode() {
        when(redisService.hasKey(RedisKey.REACTIVE_VERIFY_FAIL_LOCK.getPrefix() + "test@test.com")).thenReturn(false);
        when(redisService.get(RedisKey.REACTIVE.getPrefix() + "test@test.com")).thenReturn("123456");
        when(redisService.increment(anyString(), any())).thenReturn(1L);

        assertThatThrownBy(() -> emailVerificationService.emailReactiveVerifyCheck("test@test.com", "000000"))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }
}
