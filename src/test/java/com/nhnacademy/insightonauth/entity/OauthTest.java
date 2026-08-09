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
}
