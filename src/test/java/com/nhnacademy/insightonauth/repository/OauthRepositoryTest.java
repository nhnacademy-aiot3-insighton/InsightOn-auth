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

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User("test@test.com", "test", "01012345678");
        userRepository.save(user1);
        oauthRepository.save(new Oauth(user1, "google", "google-provider-id-123"));

        user2 = new User("other@test.com", "other", "01099998888");
        userRepository.save(user2);
        oauthRepository.save(new Oauth(user2, "google", "other-provider-id"));
    }

    @Test
    @DisplayName("user, provider로 연동 정보 조회")
    void findByUserAndProvider_returnsOauth() {
        Optional<Oauth> found = oauthRepository.findByUserAndProvider(user1, "google");

        assertThat(found)
                .isPresent()
                .get()
                .extracting(Oauth::getProviderUserId)
                .isEqualTo("google-provider-id-123");
    }

    @Test
    @DisplayName("연동 없는 provider 조회")
    void findByUserAndProvider_whenNotExists_returnsEmpty() {
        Optional<Oauth> found = oauthRepository.findByUserAndProvider(user1, "github");

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
                .isEqualTo(user1.getUserId());
    }

    @Test
    @DisplayName("없는 providerUserId 조회")
    void findByProviderAndProviderUserId_whenNotExists_returnsEmpty() {
        Optional<Oauth> found = oauthRepository.findByProviderAndProviderUserId("google", "not-exist-id");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("마스킹된 provider_user_id를 원본 접두어로 조회")
    void findByProviderAndProviderUserIdStartingWith_findsMaskedRow() {
        Oauth oauth = oauthRepository.findByUser(user1).getFirst();
        oauth.maskForWithdrawal();
        oauthRepository.saveAndFlush(oauth);

        List<Oauth> found = oauthRepository.findByProviderAndProviderUserIdStartingWith(
                "google", "google-provider-id-123;");

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getUser().getUserId()).isEqualTo(user1.getUserId());
    }

    @Test
    @DisplayName("접두어로 조회 시 다른 provider_user_id는 매칭되지 않음")
    void findByProviderAndProviderUserIdStartingWith_doesNotMatchOthers() {
        List<Oauth> found = oauthRepository.findByProviderAndProviderUserIdStartingWith(
                "google", "google-provider-id-123;");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("user로 연동 목록 조회, 다른 사용자 제외 확인")
    void findByUser_returnsOnlyOwnOauthList() {
        List<Oauth> result = oauthRepository.findByUser(user1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProvider()).isEqualTo("google");
        assertThat(result.get(0).getUser().getUserId()).isEqualTo(user1.getUserId());
    }

    @Test
    @DisplayName("user로 연동 개수 조회, 다른 사용자 제외 확인")
    void countByUser_excludesOtherUserOauths() {
        oauthRepository.save(new Oauth(user1, "github", "github-provider-id-456"));

        Long count = oauthRepository.countByUser(user1);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("연동 정보 없는 user는 개수 0 반환")
    void countByUser_whenNoOauth_returnsZero() {
        User noOauthUser = new User("nooauth@test.com", "nooauth", "01077776666");
        userRepository.save(noOauthUser);

        Long count = oauthRepository.countByUser(noOauthUser);

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("user로 연동 정보 전체 삭제, 다른 사용자 유지 확인")
    void deleteByUser_removesOnlyOwnOauths() {
        oauthRepository.save(new Oauth(user1, "github", "github-provider-id-456"));

        oauthRepository.deleteByUser(user1);

        assertThat(oauthRepository.findByUser(user1)).isEmpty();
        assertThat(oauthRepository.findByUser(user2)).hasSize(1);
    }
}
