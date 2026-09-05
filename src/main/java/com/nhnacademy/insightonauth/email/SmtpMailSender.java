package com.nhnacademy.insightonauth.email;

import com.nhnacademy.insightonauth.exception.email.EmailSendException;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpMailSender {

    private final JavaMailSender javaMailSender;

    @Value("${mail.from-address}")
    private String fromAddress;

    @Value("${mail.from-name}")
    private String fromName;

    public void send(String to, String subject, String text) {
        log.info("이메일 발송");
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
        log.info("이메일 발송 성공");
    }
}
