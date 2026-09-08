package com.nhnacademy.insightonauth.email;

import com.nhnacademy.insightonauth.exception.email.EmailSendException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpMailSenderTest {

    private JavaMailSender javaMailSender;
    private SmtpMailSender smtpMailSender;

    @BeforeEach
    void setUp() {
        javaMailSender = mock(JavaMailSender.class);
        smtpMailSender = new SmtpMailSender(javaMailSender);
        ReflectionTestUtils.setField(smtpMailSender, "fromAddress", "insighton@insighton.store");
        ReflectionTestUtils.setField(smtpMailSender, "fromName", "InsightOn");
    }

    private MimeMessage realMimeMessage() {
        return new MimeMessage(Session.getDefaultInstance(new Properties()));
    }

    @Test
    @DisplayName("send - 정상 발송")
    void send_success() {
        when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());

        assertThatCode(() -> smtpMailSender.send("user@test.com", "제목", "본문"))
                .doesNotThrowAnyException();

        verify(javaMailSender).send((MimeMessage) org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("send - 수신 주소가 올바르지 않으면 EmailSendException")
    void send_invalidAddress_throwsEmailSendException() {
        when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());

        assertThatThrownBy(() -> smtpMailSender.send("@", "제목", "본문"))
                .isInstanceOf(EmailSendException.class);
    }

    @Test
    @DisplayName("send - 메일 서버 전송 실패(타임아웃 등)면 EmailSendException")
    void send_mailException_throwsEmailSendException() {
        when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());
        doThrow(new MailSendException("timeout")).when(javaMailSender).send((MimeMessage) org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> smtpMailSender.send("user@test.com", "제목", "본문"))
                .isInstanceOf(EmailSendException.class);
    }
}
