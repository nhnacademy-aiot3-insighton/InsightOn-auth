package com.nhnacademy.insightonauth.client;

import com.nhnacademy.insightonauth.exception.UnsupportedOAuthProviderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OauthClientResolver {
    // 스프링이 bean 객체를 가져다줌
    private final Map<String, OauthClient> clients;

    public OauthClient resolve(String provider) {
        OauthClient client = clients.get(provider + "OauthClient");
        if (client == null) {
            throw new UnsupportedOAuthProviderException(provider);
        }
        return client;
    }
}
