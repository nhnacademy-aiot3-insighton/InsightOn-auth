package com.nhnacademy.insightonauth.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class UserCredentialTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
    }

    @Test
    @DisplayName("userCredential 생성자 생성 성공")
    void createUserCredential() {
        User user = new User("test@test.com", "test", "01012345678");
        UserCredential credential = new UserCredential(user, "hashed-password");

        assertThat(credential.getUser()).isEqualTo(user);
        assertThat(credential.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(credential.getCreatedAt()).isNotNull();
        assertThat(credential.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("changePassword 비밀번호, 수정시각 변경")
    void changePassword_updatesPasswordAndUpdatedAt() {
        UserCredential credential = new UserCredential(user, "old-hashed-password");
        OffsetDateTime newTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

        credential.changePassword(newTime, "new-hashed-password");

        assertThat(credential.getPasswordHash()).isEqualTo("new-hashed-password");
        assertThat(credential.getUpdatedAt()).isEqualTo(newTime);
    }
}
