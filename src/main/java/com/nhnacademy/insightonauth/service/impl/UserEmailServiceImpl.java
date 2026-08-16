package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.email.EmailService;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.exception.*;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.redis.ResendCounter;
import com.nhnacademy.insightonauth.repository.UserRepository;
import com.nhnacademy.insightonauth.service.TokenService;
import com.nhnacademy.insightonauth.service.UserCredentialService;
import com.nhnacademy.insightonauth.service.UserEmailService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Transactional
@RequiredArgsConstructor
public class UserEmailServiceImpl implements UserEmailService {

    private final EmailService emailService;
    private final RedisService redisService;
    private final ResendCounter resendCounter;
    private final TokenService tokenService;
    private final UserManagementService userManagementService;
    private final UserCredentialService userCredentialService;
    private final UserRepository userRepository;

    @Override
    public void emailVerifyRequest(String email) {
        // 1. 재전송 잠금 체크 (더 강한 제한 먼저)
        if (redisService.hasKey(RedisKey.VERIFY_RESEND_LOCK.getPrefix() + email)) {
            throw new VerificationResendLockedException("재전송 시도가 초과되어 잠겼습니다.");
        }
        // 2. 쿨다운을 원자적으로 설정
        boolean acquired = redisService.setIfAbsent(
                RedisKey.VERIFY_RESEND_COOLDOWN.getPrefix() + email,
                "1",
                Duration.ofSeconds(60)
        );

        if (!acquired) {
            throw new VerificationResendTooSoonException("잠시 후 다시 시도해 주세요.");
        }
        // 3. 카운터 증가
        // emailVerifyRequest 안에서
        boolean lockedNow = resendCounter.increase(
                        RedisKey.VERIFY_RESEND_COUNT.getPrefix() + email,
                        RedisKey.VERIFY_RESEND_LOCK.getPrefix() + email,
                        5,
                            Duration.ofMinutes(15)
                            );
        if (lockedNow) {
            throw new VerificationResendLockedException("재전송 시도가 초과되어 잠겼습니다.");
        }

        // 4. 발송
        emailService.sendVerificationCode(email);

    }

    @Override
    public String emailVerifyConfirm(String email, String code) {
        return emailService.emailCodeVerify(email, code);
    }

    @Override
    public void reactivateRequest(String email) {
        emailService.sendVerificationCode(email);
    }

    @Override
    public UserLoginResponse reactivateConfirm(String email, String code) {
        emailService.emailCodeVerify(email, code);

        User user = userManagementService.findByEmail(email);
        user.reactivate();

        return tokenService.issueTokens(user, email);
    }

    @Override
    public UserLoginResponse reactive(String reactiveToken) {
        // 바로 읽어서 있으면 삭제
        String userIdStr = redisService.getAndDelete(RedisKey.REACTIVE.getPrefix() + reactiveToken);
        if (userIdStr == null) {
            throw new InvalidReactiveTokenException("복구 요청이 유효하지 않거나 만료되었습니다.");
        }

        User user = userManagementService.findById(Long.valueOf(userIdStr));
        user.reactivate();   // 상태를 ACTIVE로, 이메일 원복

        return tokenService.issueTokens(user, user.getEmail());
    }

    // 이러면 없는 계정은 응답이 더 빨리 나가기 때문에 공격자가 계정 존재 여부를 알 수 있음
    @Override
    public void passwordResetRequest(String email) {
        // 1. 잠금 체크 — 계정 여부와 무관하게 항상
        if (redisService.hasKey(RedisKey.PASSWORD_RESET_RESEND_LOCK.getPrefix() + email)) {
            throw new PasswordResetResendLockedException("재전송 시도가 초과되어 잠겼습니다.");
        }

        // 2. 연타 방지 — hasKey+set 대신 원자적 setIfAbsent로 체크와 설정을 한 번에
        boolean acquired = redisService.setIfAbsent(
                RedisKey.PASSWORD_RESET_RESEND_COOLDOWN.getPrefix() + email,
                "1",
                Duration.ofSeconds(60)
        );
        if (!acquired) {
            throw new PasswordResetResendTooSoonException("잠시 후 다시 시도해 주세요.");
        }

        // 3. 카운터 증가 — 이번 요청으로 임계치에 도달해 잠겼으면 발송하지 않고 예외
        boolean lockedNow = resendCounter.increase(
                RedisKey.PASSWORD_RESET_RESEND_COUNT.getPrefix() + email,
                RedisKey.PASSWORD_RESET_RESEND_LOCK.getPrefix() + email,
                5,
                Duration.ofMinutes(15)
        );
        if (lockedNow) {
            throw new PasswordResetResendLockedException("재전송 시도가 초과되어 잠겼습니다.");
        }

        // 4. 실제 메일 발송만 계정 있고 정상일 때 (없어도 예외 안 던짐)
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getStatus() == Status.WITHDRAW) {
                return;   // 탈퇴 계정: 메일만 안 보냄 (rate limit은 이미 걸림)
            }
            emailService.sendPasswordResetPath(email);
        });
    }

    @Override
    public void passwordResetConfirm(String token, String newPassword) {
        String email = emailService.emailTokenVerify(token);

        User user = userManagementService.findByEmail(email);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        userCredentialService.updatePassword(now, user, newPassword);
        user.setUpdatedAt(now);
    }
}
