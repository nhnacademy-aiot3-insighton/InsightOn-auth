package com.nhnacademy.insightonauth.service.impl;

import com.nhnacademy.insightonauth.entity.Oauth;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.exception.LastLoginMethodException;
import com.nhnacademy.insightonauth.exception.OauthNotFoundException;
import com.nhnacademy.insightonauth.repository.OauthRepository;
import com.nhnacademy.insightonauth.service.OauthService;
import com.nhnacademy.insightonauth.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OauthServiceImpl implements OauthService {

    private final OauthRepository oauthRepository;
    private final UserCredentialService userCredentialService;

    @Override
    public void create(User user, String provider, String providerUserId) {
        Oauth oauth = new Oauth(user, provider, providerUserId);
        oauthRepository.save(oauth);
    }

    @Override
    public void delete(User user, Long oauthId) {
        Oauth oauth = oauthRepository.findById(oauthId)
                .orElseThrow(() -> new OauthNotFoundException("oauth를 찾을 수 없습니다."));

        boolean hasPassword = userCredentialService.exists(user);
        long oauthCount = oauthRepository.countByUser(user);

        // 비밀번호가 없고(OAuth 전용) + 남은 연동이 1개뿐일 때만 막음
        if (!hasPassword && oauthCount <= 1) {
            throw new LastLoginMethodException("마지막 남은 로그인 수단은 해제할 수 없습니다.");
        }

        oauthRepository.delete(oauth);
    }

    @Override
    public Oauth findOauth(User user, String provider) {
        return oauthRepository.findByUserAndProvider(user, provider)
                .orElseThrow(() -> new OauthNotFoundException("연동된 소셜 계정을 찾을 수 없습니다."));
    }

    @Override
    public List<Oauth> findAllByUser(User user) {
        return oauthRepository.findByUser(user);
    }
}
