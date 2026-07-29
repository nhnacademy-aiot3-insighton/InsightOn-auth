package com.nhnacademy.insightonauth.email;

import com.nhnacademy.insightonauth.exception.EmailSendException;
import com.nhnacademy.insightonauth.exception.InvalidVerificationCodeException;
import com.nhnacademy.insightonauth.redis.RedisKey;
import com.nhnacademy.insightonauth.redis.RedisService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final JavaMailSender javaMailSender;
    private final RedisService redisService;

    @Value("${mail.from-address}")
    private String fromAddress;

    @Value("${mail.from-name}")
    private String fromName;


    public void sendVerificationCode(String email) {
        // 6자리 암호화 난수 인증코드 생성
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        // Redis에 저장 (5분 TTL)
        redisService.set(RedisKey.VERIFY.getPrefix() + email, code, Duration.ofMinutes(5));

        // 메일 발송
        send(email, "[InsightOn] 이메일 인증 코드",
                "인증 코드: " + code + "\n5분 이내에 입력해 주세요.");
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
            throw new EmailSendException("이메일 발송에 실패했습니다.");
        }
    }

    public String emailVerify(String email, String inputCode) {
        String savedCode = redisService.get(RedisKey.VERIFY.getPrefix() + email);

        if (savedCode == null) {
            // 만료되었거나 애초에 요청한 적 없음
            throw new InvalidVerificationCodeException("인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        boolean codeCheck = savedCode.equals(inputCode);
        if (!codeCheck) {
            // 코드 불일치
            throw new InvalidVerificationCodeException("인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        String verificationToken = UUID.randomUUID().toString();
        redisService.set(RedisKey.VERIFIED.getPrefix() + email, verificationToken, Duration.ofMinutes(15));
        redisService.delete(RedisKey.VERIFY.getPrefix() + email);
        return verificationToken;
    }

    public boolean emailVerifyCheck(String email, String inputToken) {
        String savedToken = redisService.get(RedisKey.VERIFIED.getPrefix() + email);

        if (savedToken == null || !savedToken.equals(inputToken)) {
            throw new InvalidVerificationCodeException("인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        redisService.delete(RedisKey.VERIFIED.getPrefix() + email);
        return true;
    }
}
