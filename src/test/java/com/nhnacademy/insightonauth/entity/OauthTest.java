package com.nhnacademy.insightonauth.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OauthTest {
    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@test.com", "test", "01012345678");
    }

    @Test
    @DisplayName("oauth 생성자 생성 성공")
    void createOauth() {
        Oauth oauth = new Oauth(user, "google", "provider-user-id-123");

        assertThat(oauth.getUser()).isEqualTo(user);
        assertThat(oauth.getProvider()).isEqualTo("google");
        assertThat(oauth.getProviderUserId()).isEqualTo("provider-user-id-123");
        assertThat(oauth.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("reassignUser 호출 시 연결된 user 변경")
    void reassignUser_changesUser() {
        Oauth oauth = new Oauth(user, "google", "pid-123");
        User newUser = new User("new@test.com", "new", "01099998888");

        oauth.reassignUser(newUser);

        assertThat(oauth.getUser()).isEqualTo(newUser);
    }

    @Test
    @DisplayName("maskForWithdrawal 시 provider_user_id에 접미사 부착")
    void maskForWithdrawal_appendsSuffix() {
        Oauth oauth = new Oauth(user, "google", "pid-123");

        oauth.maskForWithdrawal();

        assertThat(oauth.getProviderUserId()).startsWith("pid-123;");
        assertThat(oauth.isMasked()).isTrue();
        assertThat(oauth.reactivatedProviderUserId()).isEqualTo("pid-123");
    }

    @Test
    @DisplayName("이미 마스킹된 경우 maskForWithdrawal은 중복 부착하지 않음")
    void maskForWithdrawal_whenAlreadyMasked_noop() {
        Oauth oauth = new Oauth(user, "google", "pid-123");
        oauth.maskForWithdrawal();
        String masked = oauth.getProviderUserId();

        oauth.maskForWithdrawal();

        assertThat(oauth.getProviderUserId()).isEqualTo(masked);
    }

    @Test
    @DisplayName("unmask 시 원본 provider_user_id로 복원")
    void unmask_restoresOriginal() {
        Oauth oauth = new Oauth(user, "google", "pid-123");
        oauth.maskForWithdrawal();

        oauth.unmask();

        assertThat(oauth.getProviderUserId()).isEqualTo("pid-123");
        assertThat(oauth.isMasked()).isFalse();
    }
}
