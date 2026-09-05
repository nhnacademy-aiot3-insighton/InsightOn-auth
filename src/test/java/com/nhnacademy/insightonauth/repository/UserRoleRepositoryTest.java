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

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User("test@test.com", "test", "01012345678");
        userRepository.save(user1);
        userRoleRepository.save(new UserRole(user1, Role.MEMBER));

        user2 = new User("other@test.com", "other", "01099998888");
        userRepository.save(user2);
        userRoleRepository.save(new UserRole(user2, Role.MEMBER));
        userRoleRepository.save(new UserRole(user2, Role.ADMIN));
    }

    @Test
    @DisplayName("user로 권한 목록 조회, 다른 사용자 제외 확인")
    void findByUser_returnsOnlyOwnRoles() {
        List<UserRole> result = userRoleRepository.findByUser(user1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(Role.MEMBER);
        assertThat(result.get(0).getUser().getUserId()).isEqualTo(user1.getUserId());
    }

    @Test
    @DisplayName("다중 권한 사용자 목록 조회")
    void findByUser_otherUserHasMultipleRoles() {
        List<UserRole> result = userRoleRepository.findByUser(user2);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("user, role로 존재 여부 확인 - true")
    void existsByUserAndRole_returnsTrueWhenExists() {
        boolean exists = userRoleRepository.existsByUserAndRole(user1, Role.MEMBER);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("user, role로 존재 여부 확인 - false")
    void existsByUserAndRole_returnsFalseWhenNotExists() {
        boolean exists = userRoleRepository.existsByUserAndRole(user1, Role.ADMIN);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("다른 사용자 역할 미포함 확인")
    void existsByUserAndRole_doesNotMixOtherUsersRole() {
        boolean user2HasAdmin = userRoleRepository.existsByUserAndRole(user2, Role.ADMIN);
        boolean user1HasAdmin = userRoleRepository.existsByUserAndRole(user1, Role.ADMIN);

        assertThat(user2HasAdmin).isTrue();
        assertThat(user1HasAdmin).isFalse();
    }

    @Test
    @DisplayName("user, role로 권한 조회")
    void findByUserAndRole_returnsUserRole() {
        Optional<UserRole> found = userRoleRepository.findByUserAndRole(user1, Role.MEMBER);

        assertThat(found)
                .isPresent()
                .get()
                .extracting(userRole -> userRole.getUser().getUserId())
                .isEqualTo(user1.getUserId());
    }

    @Test
    @DisplayName("없는 role로 조회")
    void findByUserAndRole_whenNotExists_returnsEmpty() {
        Optional<UserRole> found = userRoleRepository.findByUserAndRole(user1, Role.ADMIN);

        assertThat(found).isEmpty();
    }
}
