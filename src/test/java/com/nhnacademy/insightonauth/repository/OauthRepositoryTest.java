package com.nhnacademy.insightonauth.repository;

import com.nhnacademy.insightonauth.entity.Oauth;
import com.nhnacademy.insightonauth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@DataJpaTest
class OauthRepositoryTest {

    @Autowired
    private OauthRepository oauthRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
        userRepository.save(user);
        oauthRepository.save(new Oauth(user, "google", "google-provider-id-123"));
    }

    @Test
    @DisplayName("user, provider로 연동 정보 조회")
    void findByUserAndProvider_returnsOauth() {
        Optional<Oauth> found = oauthRepository.findByUserAndProvider(user, "google");

        assertThat(found)
                .isPresent()
                .get()
                .extracting(Oauth::getProviderUserId)
                .isEqualTo("google-provider-id-123");
    }

    @Test
    @DisplayName("연동 없는 provider 조회 시 빈 값 반환")
    void findByUserAndProvider_whenNotExists_returnsEmpty() {
        Optional<Oauth> found = oauthRepository.findByUserAndProvider(user, "github");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("provider, providerUserId로 연동 정보 조회")
    void findByProviderAndProviderUserId_returnsOauth() {
        Optional<Oauth> found = oauthRepository.findByProviderAndProviderUserId("google", "google-provider-id-123");

        assertThat(found)
                .isPresent()
                .get()
                .extracting(oauth -> oauth.getUser().getUserId())
                .isEqualTo(user.getUserId());
    }

    @Test
    @DisplayName("없는 providerUserId 조회 시 빈 값 반환")
    void findByProviderAndProviderUserId_whenNotExists_returnsEmpty() {
        Optional<Oauth> found = oauthRepository.findByProviderAndProviderUserId("google", "not-exist-id");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("user로 연동 목록 조회")
    void findByUser_returnsOauthList() {
        List<Oauth> result = oauthRepository.findByUser(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProvider()).isEqualTo("google");
    }

    @Test
    @DisplayName("user로 연동 개수 조회")
    void countByUser_returnsCount() {
        oauthRepository.save(new Oauth(user, "github", "github-provider-id-456"));

        Long count = oauthRepository.countByUser(user);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("user로 연동 정보 전체 삭제")
    void deleteByUser_removesAllOauths() {
        oauthRepository.save(new Oauth(user, "github", "github-provider-id-456"));

        oauthRepository.deleteByUser(user);

        List<Oauth> result = oauthRepository.findByUser(user);
        assertThat(result).isEmpty();
    }
}
