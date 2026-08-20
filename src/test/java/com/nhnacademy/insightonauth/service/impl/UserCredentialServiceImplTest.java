package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserCredential;
import com.nhnacademy.insightonauth.exception.UserCredentialsNotFoundException;
import com.nhnacademy.insightonauth.repository.UserCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCredentialServiceImplTest {

    @Mock
    private UserCredentialRepository userCredentialRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserCredentialServiceImpl userCredentialService;

    User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
        ReflectionTestUtils.setField(user, "userId", 1L);
    }

    @Test
    @DisplayName("비밀번호 암호화되어 저장됨")
    void create() {
        when(passwordEncoder.encode("test1234!")).thenReturn("!1234test");

        userCredentialService.create(user, "test1234!");

        verify(passwordEncoder, times(1)).encode(anyString());
        verify(userCredentialRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("자격 증명이 있으면 조회됨")
    void findByUser_exists() {
        UserCredential userCredential = mock(UserCredential.class);
        when(userCredentialRepository.findByUser(user)).thenReturn(Optional.of(userCredential));

        UserCredential result = userCredentialService.findByUser(user);

        assertThat(result).isEqualTo(userCredential);
    }

    @Test
    @DisplayName("자격 증명이 없으면 예외 발생")
    void findByUser_notExists() {
        when(userCredentialRepository.findByUser(user))
                .thenThrow(new UserCredentialsNotFoundException("유저 인증 정보가 없습니다."));

        assertThatThrownBy(() -> userCredentialService.findByUser(user))
                .isInstanceOf(UserCredentialsNotFoundException.class)
                .hasMessage("유저 인증 정보가 없습니다.");
    }

    @Test
    @DisplayName("자격 증명 삭제됨")
    void delete() {
        UserCredential userCredential = mock(UserCredential.class);
        when(userCredentialRepository.findByUser(user)).thenReturn(Optional.of(userCredential));

        userCredentialService.delete(user);

        verify(userCredentialRepository).delete(userCredential);
    }

    @Test
    @DisplayName("비밀번호 변경됨")
    void updatePassword() {
        UserCredential userCredential = mock(UserCredential.class);
        when(userCredentialRepository.findByUser(user)).thenReturn(Optional.of(userCredential));
        when(passwordEncoder.encode("test1234!")).thenReturn("!1234test");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        userCredentialService.updatePassword(now, user, "test1234!");

        verify(userCredential, times(1)).changePassword(now, "!1234test");
    }

    @Test
    @DisplayName("자격 증명이 있으면 true 반환")
    void exists_true() {
        when(userCredentialRepository.existsByUser(user)).thenReturn(true);

        boolean result = userCredentialService.exists(user);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("자격 증명이 없으면 false 반환")
    void exists_false() {
        when(userCredentialRepository.existsByUser(user)).thenReturn(false);

        boolean result = userCredentialService.exists(user);

        assertThat(result).isFalse();
    }
}
