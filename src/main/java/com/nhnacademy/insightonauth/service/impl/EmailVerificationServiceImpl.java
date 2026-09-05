package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.email.SmtpMailSender;
import com.nhnacademy.insightonauth.exception.email.InvalidVerificationCodeException;
import com.nhnacademy.insightonauth.exception.email.InvalidVerificationTokenException;
import com.nhnacademy.insightonauth.exception.email.VerificationTemporarilyLockedException;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import com.nhnacademy.insightonauth.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    // SecureRandom 생성 비용이 커서 매번 새로 만들지 않고 재사용한다 (스레드 안전이라 공유 가능).
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SmtpMailSender mailSender;
    private final RedisService redisService;

    // dev: http://localhost:8400, prod: https://insighton.store (OauthWebSupport와 동일한 프로퍼티)
    @Value("${app.front-url}")
    private String frontUrl;

    @Override
    public void sendVerificationCode(String email) {
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        redisService.set(RedisKey.VERIFY.getPrefix() + email, code, Duration.ofMinutes(5));

        mailSender.send(email, "[InsightOn] 이메일 인증 코드",
                "인증 코드: " + code + "\n5분 이내에 입력해 주세요.");
    }

    @Override
    public void sendPasswordResetPath(String email) {
        // 역방향 키로 이 이메일의 기존 토큰을 찾아 예전 링크를 무효화
        String oldUuid = redisService.get(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + email);
        if (oldUuid != null && !oldUuid.isBlank()) {
            redisService.delete(RedisKey.PASSWORD_RESET.getPrefix() + oldUuid);
        }

        String uuid = UUID.randomUUID().toString();
        String path = frontUrl + "/password/reset?token=" + uuid;

        redisService.set(RedisKey.PASSWORD_RESET.getPrefix() + uuid, email, Duration.ofMinutes(10));
        redisService.set(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + email, uuid, Duration.ofMinutes(10));

        mailSender.send(email, "[InsightOn] 비밀번호 재설정",
                "비밀번호 재설정 경로: " + path + "\n10분 이내에 수정해 주세요.");
    }

    @Override
    public void sendReactiveVerificationCode(String email) {
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        redisService.set(RedisKey.REACTIVE.getPrefix() + email, code, Duration.ofMinutes(5));

        mailSender.send(email, "[InsightOn] 이메일 인증 코드",
                "인증 코드: " + code + "\n5분 이내에 입력해 주세요.");
    }

    @Override
    public String emailCodeVerify(String email, String inputCode) {
        if (redisService.hasKey(RedisKey.VERIFY_FAIL_LOCK.getPrefix() + email)) {
            throw new VerificationTemporarilyLockedException("인증 시도가 5회 초과되어 5분간 잠겼습니다.");
        }

        String savedCode = redisService.get(RedisKey.VERIFY.getPrefix() + email);

        if (savedCode == null || !savedCode.equals(inputCode)) {
            increaseVerifyFailCount(RedisKey.VERIFY_FAIL, RedisKey.VERIFY_FAIL_LOCK, email);
            throw new InvalidVerificationCodeException("인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        redisService.delete(RedisKey.VERIFY_FAIL.getPrefix() + email);
        redisService.delete(RedisKey.VERIFY.getPrefix() + email);

        String verificationToken = UUID.randomUUID().toString();
        redisService.set(RedisKey.VERIFIED.getPrefix() + email, verificationToken, Duration.ofMinutes(15));
        return verificationToken;
    }

    @Override
    public boolean emailVerifyCheck(String email, String inputToken) {
        String savedToken = redisService.get(RedisKey.VERIFIED.getPrefix() + email);

        if (savedToken == null || !savedToken.equals(inputToken)) {
            throw new InvalidVerificationCodeException("인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        redisService.delete(RedisKey.VERIFIED.getPrefix() + email);
        return true;
    }

    @Override
    public String emailTokenVerify(String token) {
        // 조회만 하고 지우지 않는다 — 여기서 지워버리면 뒤이은 비밀번호 변경이
        // (동일 비밀번호 등으로) 실패했을 때 토큰이 이미 소모돼 재시도가 불가능해진다.
        String savedEmail = redisService.get(RedisKey.PASSWORD_RESET.getPrefix() + token);
        if (savedEmail == null || savedEmail.isBlank()) {
            throw new InvalidVerificationTokenException("인증 토큰이 올바르지 않거나 만료되었습니다.");
        }

        return savedEmail;
    }

    @Override
    public void consumePasswordResetToken(String token, String email) {
        // 비밀번호 변경이 실제로 성공한 뒤에만 호출 — 정방향 토큰 삭제
        redisService.delete(RedisKey.PASSWORD_RESET.getPrefix() + token);

        // 역방향 키는 "아직 이 token을 가리킬 때만" 삭제 — 다르면 그 사이 새 토큰이 발급된 것이므로 최신 역방향은 건드리지 않음
        String currentUuid = redisService.get(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + email);
        if (token.equals(currentUuid)) {
            redisService.delete(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + email);
        }
    }

    @Override
    public void emailReactiveVerifyCheck(String email, String inputCode) {
        if (redisService.hasKey(RedisKey.REACTIVE_VERIFY_FAIL_LOCK.getPrefix() + email)) {
            throw new VerificationTemporarilyLockedException("인증 시도가 5회 초과되어 5분간 잠겼습니다.");
        }

        String savedCode = redisService.get(RedisKey.REACTIVE.getPrefix() + email);

        if (savedCode == null || !savedCode.equals(inputCode)) {
            increaseVerifyFailCount(RedisKey.REACTIVE_VERIFY_FAIL, RedisKey.REACTIVE_VERIFY_FAIL_LOCK, email);
            throw new InvalidVerificationCodeException("인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        redisService.delete(RedisKey.REACTIVE_VERIFY_FAIL.getPrefix() + email);
        redisService.delete(RedisKey.REACTIVE.getPrefix() + email);
    }

    private void increaseVerifyFailCount(RedisKey failKey, RedisKey lockKey, String email) {
        // 원자적 INCR — 병렬 오입력이 같은 값을 읽고 덮어써 잠금을 건너뛰는 것을 막는다.
        long failCount = redisService.increment(failKey.getPrefix() + email, Duration.ofMinutes(5));

        if (failCount >= 5) {
            redisService.delete(failKey.getPrefix() + email);
            redisService.set(lockKey.getPrefix() + email, "locked", Duration.ofMinutes(5));
            throw new VerificationTemporarilyLockedException("인증 시도가 5회 초과되어 5분간 잠겼습니다.");
        }
    }
}
