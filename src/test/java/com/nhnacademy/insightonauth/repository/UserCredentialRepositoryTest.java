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

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
        userRepository.save(user);
        userCredentialRepository.save(new UserCredential(user, "hashed-password"));
    }

    @Test
    @DisplayName("user로 존재 여부 확인 - true")
    void existsByUser_returnsTrueWhenExists() {
        boolean exists = userCredentialRepository.existsByUser(user);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("user로 존재 여부 확인 - false")
    void existsByUser_returnsFalseWhenNotExists() {
        User otherUser = new User("other@test.com", "other", "01099998888");
        userRepository.save(otherUser);

        boolean exists = userCredentialRepository.existsByUser(otherUser);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("user로 인증 정보 조회")
    void findByUser_returnsUserCredential() {
        Optional<UserCredential> found = userCredentialRepository.findByUser(user);

        assertThat(found)
                .isPresent()
                .get()
                .extracting(UserCredential::getPasswordHash)
                .isEqualTo("hashed-password");
    }

    @Test
    @DisplayName("인증 정보 없는 user 조회 시 빈 값 반환")
    void findByUser_whenNotExists_returnsEmpty() {
        User otherUser = new User("other@test.com", "other", "01099998888");
        userRepository.save(otherUser);

        Optional<UserCredential> found = userCredentialRepository.findByUser(otherUser);

        assertThat(found).isEmpty();
    }
}
