package com.nhnacademy.insightonauth.repository;

import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User("test@test.com", "test", "01012345678");
        userRepository.save(activeUser);
    }

    @Test
    @DisplayName("email로 유저 조회")
    void findByEmail_returnsUser() {
        Optional<User> found = userRepository.findByEmail("test@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUserName()).isEqualTo("test");
    }

    @Test
    @DisplayName("없는 email 조회 시 빈 값 반환")
    void findByEmail_whenNotExists_returnsEmpty() {
        Optional<User> found = userRepository.findByEmail("notfound@test.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("userName, phoneNumber로 유저 조회")
    void findByUserNameAndPhoneNumber_returnsUser() {
        Optional<User> found = userRepository.findByUserNameAndPhoneNumber("test", "01012345678");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("탈퇴 email 접두어, status로 유저 조회")
    void findByEmailStartingWithAndStatus_returnsUser() {
        activeUser.withdraw();
        userRepository.save(activeUser);
        String originalEmailPrefix = "test@test.com;";

        Optional<User> found = userRepository.findByEmailStartingWithAndStatus(originalEmailPrefix, Status.WITHDRAW);

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("email, userName, status로 페이징 조회")
    void findByEmailContainingAndUserNameContainingAndStatus_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<User> result = userRepository.findByEmailContainingAndUserNameContainingAndStatus(
                "test", "test", Status.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("email, userName으로 페이징 조회")
    void findByEmailContainingAndUserNameContaining_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<User> result = userRepository.findByEmailContainingAndUserNameContaining(
                "test", "test", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("email 존재 여부 확인 - true")
    void existsByEmail_returnsTrueWhenExists() {
        boolean exists = userRepository.existsByEmail("test@test.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("email 존재 여부 확인 - false")
    void existsByEmail_returnsFalseWhenNotExists() {
        boolean exists = userRepository.existsByEmail("notfound@test.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("phoneNumber 존재 여부 확인")
    void existsByPhoneNumber_returnsTrueWhenExists() {
        boolean exists = userRepository.existsByPhoneNumber("01012345678");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("탈퇴 90일 경과 유저 조회")
    void findByStatusAndWithdrawnAtBefore_returnsExpiredWithdrawnUsers() {
        activeUser.withdraw();
        activeUser.setWithdrawnAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(100));
        userRepository.save(activeUser);

        List<User> result = userRepository.findByStatusAndWithdrawnAtBefore(
                Status.WITHDRAW, OffsetDateTime.now(ZoneOffset.UTC).minusDays(90));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("탈퇴 90일 미경과 유저 제외")
    void findByStatusAndWithdrawnAtBefore_excludesRecentWithdrawnUsers() {
        activeUser.withdraw();
        userRepository.save(activeUser);

        List<User> result = userRepository.findByStatusAndWithdrawnAtBefore(
                Status.WITHDRAW, OffsetDateTime.now(ZoneOffset.UTC).minusDays(90));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("30일 미접속 유저 조회")
    void findByStatusAndLastLoginAtBefore_returnsInactiveUsers() {
        activeUser.updateLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(40));
        userRepository.save(activeUser);

        List<User> result = userRepository.findByStatusAndLastLoginAtBefore(
                Status.ACTIVE, OffsetDateTime.now(ZoneOffset.UTC).minusDays(30));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("최근 접속 유저 제외")
    void findByStatusAndLastLoginAtBefore_excludesRecentlyActiveUsers() {
        activeUser.updateLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));
        userRepository.save(activeUser);

        List<User> result = userRepository.findByStatusAndLastLoginAtBefore(
                Status.ACTIVE, OffsetDateTime.now(ZoneOffset.UTC).minusDays(30));

        assertThat(result).isEmpty();
    }
}

