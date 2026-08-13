package com.nhnacademy.insightonauth.entity;

import com.nhnacademy.insightonauth.exception.InvalidUserStatusException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("BLOCK 상태에서 reactivate 호출 시 예외 발생")
    void reactivate_whenBlocked_throwsException() {
        User user = new User("test@test.com", "test", "01012345678");
        user.setStatus(Status.BLOCK);

        assertThatThrownBy(user::reactivate)
                .isInstanceOf(InvalidUserStatusException.class)
                .hasMessage("휴면 또는 탈퇴 상태가 아닙니다.");
    }

}
