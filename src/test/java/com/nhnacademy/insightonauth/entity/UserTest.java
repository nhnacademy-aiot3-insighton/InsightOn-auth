package com.nhnacademy.insightonauth.entity;

import com.nhnacademy.insightonauth.exception.InvalidUserStatusException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class UserTest {

    @Test
    @DisplayName("user 생성자 생성 성공")
    void createUser() {
        User user = new User("test@test.com", "test", "01012345678");

        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getUserName()).isEqualTo("test");
        assertThat(user.getPhoneNumber()).isEqualTo("01012345678");
        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    @DisplayName("user 생성 직후 시각 필드 초기화 상태 확인")
    void createUser_initializesTimeFields() {
        User user = new User("test@test.com", "test", "01012345678");

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("withdraw시 접미사 확인")
    void withdraw_suffix() {
        User user = new User("test@test.com", "test", "01012345678");

        user.withdraw();

        assertThat(user.getEmail()).startsWith("test@test.com;");
        assertThat(user.getPhoneNumber()).startsWith("01012345678;");
        assertThat(user.getStatus()).isEqualTo(Status.WITHDRAW);
        assertThat(user.getWithdrawnAt()).isNotNull();
    }

    @Test
    @DisplayName("withdraw시 updatedAt이 withdrawnAt과 함께 갱신")
    void withdraw_updatesUpdatedAt() {
        User user = new User("test@test.com", "test", "01012345678");
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        user.withdraw();

        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(before);
        assertThat(user.getUpdatedAt()).isEqualTo(user.getWithdrawnAt());
    }

    @Test
    @DisplayName("withdraw후 reactivate시 원복")
    void withdraw_then_reactivate_restoresEmail() {
        User user = new User("test@test.com", "test", "01012345678");
        user.withdraw();

        user.reactivate();

        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getPhoneNumber()).isEqualTo("01012345678");
        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(user.getWithdrawnAt()).isNull();
    }

    @Test
    @DisplayName("전화번호 null 계정도 withdraw 후 reactivate 정상 동작")
    void withdraw_then_reactivate_withNullPhoneNumber() {
        User user = new User("test@test.com", "test", null);
        user.withdraw();

        user.reactivate();

        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(user.getWithdrawnAt()).isNull();
    }

    @Test
    @DisplayName("이메일에 ';'가 포함돼도 withdraw 후 reactivate가 원본 이메일 복원")
    void withdraw_then_reactivate_whenEmailContainsSemicolon() {
        User user = new User("a;b@test.com", "test", "01012345678");
        user.withdraw();

        user.reactivate();

        assertThat(user.getEmail()).isEqualTo("a;b@test.com");
        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    @DisplayName("withdraw후 reactivate시 lastLoginAt, updatedAt 갱신")
    void withdraw_then_reactivate_updatesTimeFields() {
        User user = new User("test@test.com", "test", "01012345678");
        user.withdraw();
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        user.reactivate();

        assertThat(user.getLastLoginAt()).isAfterOrEqualTo(before);
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("ACTIVE 상태에서 reactivate 호출 시 예외 발생")
    void reactivate_whenActive_throwsException() {
        User user = new User("test@test.com", "test", "01012345678");

        assertThatThrownBy(user::reactivate)
                .isInstanceOf(InvalidUserStatusException.class)
                .hasMessage("휴면 또는 탈퇴 상태가 아닙니다.");
    }

    @Test
    @DisplayName("전화번호가 null이어도 withdraw 정상 동작")
    void withdraw_withNullPhoneNumber_worksCorrectly() {
        User user = new User("test@test.com", "test", null);
        user.withdraw();

        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getStatus()).isEqualTo(Status.WITHDRAW);
    }

    @Test
    @DisplayName("SLEEP 상태에서 reactivate 호출 시 ACTIVE로 전환")
    void reactivate_whenSleep_becomesActive() {
        User user = new User("test@test.com", "test", "01012345678");
        user.setStatus(Status.SLEEP);

        user.reactivate();

        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(user.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("SLEEP 상태에서 reactivate 호출 시 withdrawnAt은 null 유지, 시각 필드 갱신")
    void reactivate_whenSleep_keepsWithdrawnAtNullAndUpdatesTimeFields() {
        User user = new User("test@test.com", "test", "01012345678");
        user.setStatus(Status.SLEEP);
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        user.reactivate();

        assertThat(user.getWithdrawnAt()).isNull();
        assertThat(user.getLastLoginAt()).isAfterOrEqualTo(before);
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("BLOCK 상태에서 reactivate 호출 시 예외 발생")
    void reactivate_whenBlocked_throwsException() {
        User user = new User("test@test.com", "test", "01012345678");
        user.setStatus(Status.BLOCK);

        assertThatThrownBy(user::reactivate)
                .isInstanceOf(InvalidUserStatusException.class)
                .hasMessage("휴면 또는 탈퇴 상태가 아닙니다.");
    }

    @Test
    @DisplayName("updateLastLoginAt 호출 시 현재 시각으로 설정")
    void updateLastLoginAt_setsTime() {
        User user = new User("test@test.com", "test", "01012345678");
        assertThat(user.getLastLoginAt()).isNull();
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        user.updateLastLoginAt();

        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(user.getLastLoginAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("updateLastLoginAt 시각 지정 호출 시 해당 시각으로 설정")
    void updateLastLoginAt_withGivenTime() {
        User user = new User("test@test.com", "test", "01012345678");
        OffsetDateTime t = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        user.updateLastLoginAt(t);

        assertThat(user.getLastLoginAt()).isEqualTo(t);
    }
}
