package com.nhnacademy.insightonauth.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class UserRoleTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
    }

    @Test
    @DisplayName("userRole 생성자 생성 성공")
    void createUserRole() {
        UserRole userRole = new UserRole(user, Role.MEMBER);

        assertThat(userRole.getUser()).isEqualTo(user);
        assertThat(userRole.getRole()).isEqualTo(Role.MEMBER);
        assertThat(userRole.getCreatedAt()).isNotNull();
    }

}
