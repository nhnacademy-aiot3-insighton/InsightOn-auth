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
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("탈퇴 후에는 원본 email로 조회되지 않음")
    void findByEmail_afterWithdraw_originalEmailNotMatched() {
        activeUser.withdraw();
        userRepository.save(activeUser);

        Optional<User> found = userRepository.findByEmail("test@test.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("email 중복 저장 시 제약 위반 예외 발생")
    void save_duplicateEmail_throwsException() {
        User duplicate = new User("test@test.com", "another", "01055556666");

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("phoneNumber 중복 저장 시 제약 위반 예외 발생")
    void save_duplicatePhoneNumber_throwsException() {
        User duplicate = new User("another@test.com", "another", "01012345678");

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("userName, phoneNumber로 유저 조회")
    void findByUserNameAndPhoneNumber_returnsUser() {
        Optional<User> found = userRepository.findByUserNameAndPhoneNumber("test", "01012345678");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("userName, phoneNumber 불일치 시 빈 값 반환")
    void findByUserNameAndPhoneNumber_whenNotMatched_returnsEmpty() {
        Optional<User> found = userRepository.findByUserNameAndPhoneNumber("test", "09999999999");

        assertThat(found).isEmpty();
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
    @DisplayName("email 접두어는 맞지만 status 불일치 시 빈 값 반환")
    void findByEmailStartingWithAndStatus_whenStatusMismatch_returnsEmpty() {
        Optional<User> found = userRepository.findByEmailStartingWithAndStatus("test@test.com", Status.WITHDRAW);

        assertThat(found).isEmpty();
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
    @DisplayName("email, userName, status 조회 시 status 불일치 유저 제외")
    void findByEmailContainingAndUserNameContainingAndStatus_excludesOtherStatus() {
        User sleepUser = new User("test-sleep@test.com", "test-sleep", "01011112222");
        sleepUser.setStatus(Status.SLEEP);
        userRepository.save(sleepUser);

        Page<User> result = userRepository.findByEmailContainingAndUserNameContainingAndStatus(
                "test", "test", Status.ACTIVE, PageRequest.of(0, 10));

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
    @DisplayName("email, userName 페이징 - 페이지 크기/전체 건수/페이지 수 검증")
    void findByEmailContainingAndUserNameContaining_paginatesResults() {
        userRepository.save(new User("test2@test.com", "tester2", "01022223333"));
        userRepository.save(new User("test3@test.com", "tester3", "01033334444"));

        Page<User> firstPage = userRepository.findByEmailContainingAndUserNameContaining(
                "test", "test", PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.isFirst()).isTrue();
        assertThat(firstPage.hasNext()).isTrue();

        Page<User> secondPage = userRepository.findByEmailContainingAndUserNameContaining(
                "test", "test", PageRequest.of(1, 2));

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.isLast()).isTrue();
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
    @DisplayName("phoneNumber 존재 여부 확인 - false")
    void existsByPhoneNumber_returnsFalseWhenNotExists() {
        boolean exists = userRepository.existsByPhoneNumber("09999999999");

        assertThat(exists).isFalse();
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

