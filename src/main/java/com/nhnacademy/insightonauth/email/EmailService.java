package com.nhnacademy.insightonauth.email;

import com.nhnacademy.insightonauth.exception.*;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final RedisService redisService;

    @Value("${mail.from-address}")
    private String fromAddress;

    @Value("${mail.from-name}")
    private String fromName;


    // 이메일 확인 코드 발송
    public void sendVerificationCode(String email) {
        // 6자리 암호화 난수 인증코드 생성
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        // Redis에 저장 (5분 TTL)
        redisService.set(RedisKey.VERIFY.getPrefix() + email, code, Duration.ofMinutes(5));

        // 메일 발송
        send(email, "[InsightOn] 이메일 인증 코드",
                "인증 코드: " + code + "\n5분 이내에 입력해 주세요.");
    }

    // 비밀번호 재설정 경로 발성
    public void sendPasswordResetPath(String email) {
        // 1. 역방향 키로 이 이메일의 기존 토큰을 찾아 예전 링크를 무효화
        String oldUuid = redisService.get(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + email);
        if (oldUuid != null && !oldUuid.isBlank()) {
            redisService.delete(RedisKey.PASSWORD_RESET.getPrefix() + oldUuid);
        }

        // 2. 새 토큰 발급
        String uuid = UUID.randomUUID().toString();
        String path = "https://insighton.store/password/reset?token=" + uuid;

        // 3. 정방향 키 저장 (uuid → email, 10분 TTL)
        redisService.set(RedisKey.PASSWORD_RESET.getPrefix() + uuid, email, Duration.ofMinutes(10));

        // 4. 역방향 키 갱신 (email → uuid, 같은 TTL)
        redisService.set(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + email, uuid, Duration.ofMinutes(10));

        // 5. 메일 발송
        send(email, "[InsightOn] 비밀번호 재설정",
                "비밀번호 재설정 경로: " + path + "\n10분 이내에 수정해 주세요.");
    }

    private void send(String to, String subject, String text) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress, fromName);   // "InsightOn <insighton@insighton.store>"
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);   // false = 순수 텍스트, true면 HTML

            javaMailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new EmailSendException("이메일 발송에 실패했습니다.", e);
        } catch (MailException e) {
            log.error("이메일 전송 실패(타임아웃 등) - to: {}, error: {}", to, e.getMessage());
            throw new EmailSendException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    // 이메일 코드 확인
    public String emailCodeVerify(String email, String inputCode) {
        // 입력 실패 잠금 체크 (검증 전용)
        if (redisService.hasKey(RedisKey.VERIFY_FAIL_LOCK.getPrefix() + email)) {
            throw new VerificationTemporarilyLockedException("인증 시도가 5회 초과되어 5분간 잠겼습니다.");
        }

        String savedCode = redisService.get(RedisKey.VERIFY.getPrefix() + email);

        if (savedCode == null || !savedCode.equals(inputCode)) {
            increaseVerifyFailCount(email);   // 입력 실패 카운트 (여기서 5회 넘으면 VERIFY_FAIL_LOCK 걸림)
            throw new InvalidVerificationCodeException("인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        // 성공 처리
        redisService.delete(RedisKey.VERIFY_FAIL.getPrefix() + email);
        redisService.delete(RedisKey.VERIFY.getPrefix() + email);

        String verificationToken = UUID.randomUUID().toString();
        redisService.set(RedisKey.VERIFIED.getPrefix() + email, verificationToken, Duration.ofMinutes(15));
        return verificationToken;
    }

    // 회원가입시 최종 확인된 이메일인지 다시 확인
    public boolean emailVerifyCheck(String email, String inputToken) {
        String savedToken = redisService.get(RedisKey.VERIFIED.getPrefix() + email);

        if (savedToken == null || !savedToken.equals(inputToken)) {
            throw new InvalidVerificationCodeException("인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        redisService.delete(RedisKey.VERIFIED.getPrefix() + email);
        return true;
    }

    public String emailTokenVerify(String token) {
        String savedEmail = redisService.get(RedisKey.PASSWORD_RESET.getPrefix() + token);

        // 토큰 만료 또는 존재하지 않는 토큰
        if (savedEmail == null || savedEmail.isBlank()) {
            throw new InvalidVerificationTokenException("인증 토큰이 올바르지 않거나 만료되었습니다.");
        }

        // 정방향 + 역방향 키 모두 삭제 (토큰 1회용, 재설정 완료 후 정리)
        redisService.delete(RedisKey.PASSWORD_RESET.getPrefix() + token);
        redisService.delete(RedisKey.PASSWORD_RESET_BY_EMAIL.getPrefix() + savedEmail);

        return savedEmail;
    }

    private void increaseVerifyFailCount(String email) {
        String saved = redisService.get(RedisKey.VERIFY_FAIL.getPrefix() + email);
        int failCount = (saved == null || saved.isBlank()) ? 0 : Integer.parseInt(saved);
        failCount++;

        if (failCount >= 5) {
            redisService.delete(RedisKey.VERIFY_FAIL.getPrefix() + email);
            redisService.set(RedisKey.VERIFY_FAIL_LOCK.getPrefix() + email, "locked", Duration.ofMinutes(5));
            throw new VerificationTemporarilyLockedException("인증 시도가 5회 초과되어 5분간 잠겼습니다.");
        } else {
            redisService.set(RedisKey.VERIFY_FAIL.getPrefix() + email, String.valueOf(failCount), Duration.ofMinutes(5));
        }
    }
}
