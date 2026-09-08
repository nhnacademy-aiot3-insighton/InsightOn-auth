package com.nhnacademy.insightonauth.client;

import com.nhnacademy.insightonauth.exception.oauth.UnsupportedOAuthProviderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OauthClientResolverTest {

    @Test
    @DisplayName("provider + \"OauthClient\" 빈 이름으로 클라이언트를 찾는다")
    void resolve_found() {
        OauthClient googleClient = mock(OauthClient.class);
        OauthClientResolver resolver = new OauthClientResolver(Map.of("googleOauthClient", googleClient));

        assertThat(resolver.resolve("google")).isSameAs(googleClient);
    }

    @Test
    @DisplayName("등록 안 된 provider면 예외")
    void resolve_notFound() {
        OauthClientResolver resolver = new OauthClientResolver(Map.of());

        assertThatThrownBy(() -> resolver.resolve("kakao"))
                .isInstanceOf(UnsupportedOAuthProviderException.class);
    }
}
