package com.nhnacademy.insightonauth.repository;

import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
class UserCredentialRepositoryTest {
    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User("test@test.com", "test", "01012345678");
        userRepository.save(user1);
        userCredentialRepository.save(new UserCredential(user1, "hashed-password"));

        user2 = new User("other@test.com", "other", "01099998888");
        userRepository.save(user2);
        userCredentialRepository.save(new UserCredential(user2, "other-hashed-password"));
    }

    @Test
    @DisplayName("user로 존재 여부 확인 - true")
    void existsByUser_returnsTrueWhenExists() {
        boolean exists = userCredentialRepository.existsByUser(user1);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("user로 존재 여부 확인 - false")
    void existsByUser_returnsFalseWhenNotExists() {
        User noCredentialUser = new User("nocred@test.com", "nocred", "01077776666");
        userRepository.save(noCredentialUser);

        boolean exists = userCredentialRepository.existsByUser(noCredentialUser);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("user로 인증 정보 조회")
    void findByUser_returnsOwnUserCredential() {
        Optional<UserCredential> found = userCredentialRepository.findByUser(user1);

        assertThat(found)
                .isPresent()
                .get()
                .extracting(UserCredential::getPasswordHash)
                .isEqualTo("hashed-password");
    }

    @Test
    @DisplayName("다른 사용자 인증 정보 조회")
    void findByUser_returnsOtherUsersOwnCredential() {
        Optional<UserCredential> found = userCredentialRepository.findByUser(user2);

        assertThat(found)
                .isPresent()
                .get()
                .extracting(UserCredential::getPasswordHash)
                .isEqualTo("other-hashed-password");
    }

    @Test
    @DisplayName("인증 정보 없는 user 조회")
    void findByUser_whenNotExists_returnsEmpty() {
        User noCredentialUser = new User("nocred@test.com", "nocred", "01077776666");
        userRepository.save(noCredentialUser);

        Optional<UserCredential> found = userCredentialRepository.findByUser(noCredentialUser);

        assertThat(found).isEmpty();
    }
}
