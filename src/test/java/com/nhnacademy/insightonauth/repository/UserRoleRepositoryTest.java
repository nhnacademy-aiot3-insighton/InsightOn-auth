package com.nhnacademy.insightonauth.repository;

import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
class UserRoleRepositoryTest {

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
        userRepository.save(user);
        userRoleRepository.save(new UserRole(user, Role.MEMBER));
    }

    @Test
    @DisplayName("user로 권한 목록 조회")
    void findByUser_returnsRoles() {
        List<UserRole> result = userRoleRepository.findByUser(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(Role.MEMBER);
    }

    @Test
    @DisplayName("user, role로 존재 여부 확인 - true")
    void existsByUserAndRole_returnsTrueWhenExists() {
        boolean exists = userRoleRepository.existsByUserAndRole(user, Role.MEMBER);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("user, role로 존재 여부 확인 - false")
    void existsByUserAndRole_returnsFalseWhenNotExists() {
        boolean exists = userRoleRepository.existsByUserAndRole(user, Role.ADMIN);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("user, role로 권한 조회")
    void findByUserAndRole_returnsUserRole() {
        Optional<UserRole> found = userRoleRepository.findByUserAndRole(user, Role.MEMBER);

        assertThat(found).isPresent();
        assertThat(found.get().getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("없는 role로 조회 시 빈 값 반환")
    void findByUserAndRole_whenNotExists_returnsEmpty() {
        Optional<UserRole> found = userRoleRepository.findByUserAndRole(user, Role.ADMIN);

        assertThat(found).isEmpty();
    }
}
